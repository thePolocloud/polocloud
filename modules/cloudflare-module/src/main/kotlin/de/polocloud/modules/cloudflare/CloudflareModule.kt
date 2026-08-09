package de.polocloud.modules.cloudflare

import de.polocloud.api.Polocloud
import de.polocloud.api.group.GroupFilterType
import de.polocloud.moduleapi.PolocloudModule
import de.polocloud.moduleapi.config.ModuleConfig
import de.polocloud.moduleapi.config.config
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.event.server.ServiceOnlineEvent
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer


class CloudflareModule : PolocloudModule() {

    private lateinit var config: ModuleConfig<CloudflareConfig>
    private var client: CloudflareClient? = null
    private var scheduler: ScheduledExecutorService? = null

    private val onlineListener = Consumer<ServiceOnlineEvent> { event ->
        runCatching { onServiceOnline(event.service) }
            .onFailure { logger.error("Failed to register '{}' with Cloudflare: {}", event.service.name(), it.message, it) }
    }

    private val stoppedListener = Consumer<ServerStoppedEvent> { event ->
        runCatching { onServiceStopped(event.service) }
            .onFailure { logger.error("Failed to remove '{}' from Cloudflare: {}", event.service.name(), it.message, it) }
    }

    override fun onLoad() {
        config = node.config { CloudflareConfig() }
    }

    override fun onEnable() {
        val cfg = config.value
        if (cfg.apiToken.isBlank() || cfg.zoneId.isBlank() || cfg.record.isBlank()) {
            error(
                "not configured — set apiToken/zoneId/record in ${dataFolder.resolve("config.yml")}, " +
                    "then run 'module enable cloudflare' again"
            )
        }

        client = CloudflareClient(cfg.apiToken, cfg.zoneId)

        Polocloud.eventService.subscribe(ServiceOnlineEvent::class.java, onlineListener)
        Polocloud.eventService.subscribe(ServerStoppedEvent::class.java, stoppedListener)

        reconcile()

        val executor = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "cloudflare-module-reconcile").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay(
            { runCatching { reconcile() }.onFailure { logger.error("Cloudflare reconciliation failed: {}", it.message, it) } },
            cfg.reconcileIntervalSeconds, cfg.reconcileIntervalSeconds, TimeUnit.SECONDS,
        )
        scheduler = executor
    }

    override fun onDisable() {
        scheduler?.shutdownNow()
        scheduler = null

        Polocloud.eventService.unsubscribe(ServiceOnlineEvent::class.java, onlineListener)
        Polocloud.eventService.unsubscribe(ServerStoppedEvent::class.java, stoppedListener)

        client = null
    }

    private fun onServiceOnline(service: Service) {
        val cf = client ?: return
        if (!node.isHead() || !isProxy(service)) return

        val cfg = config.value
        val comment = commentFor(service)
        val alreadyPresent = cf.listRecords(cfg.record).any { it.comment == comment }
        if (alreadyPresent) return

        cf.createRecord(CloudflareRecord(name = cfg.record, content = service.host, ttl = cfg.ttl, proxied = cfg.proxied, comment = comment))
        logger.info("Registered proxy '{}' ({}) with Cloudflare", service.name(), service.host)
    }

    private fun onServiceStopped(service: Service) {
        val cf = client ?: return
        if (!node.isHead()) return

        val cfg = config.value
        val comment = commentFor(service)
        val removed = cf.listRecords(cfg.record).filter { it.comment == comment }
        removed.forEach { record -> record.id?.let(cf::deleteRecord) }
        if (removed.isNotEmpty()) {
            logger.info("Removed proxy '{}' from Cloudflare", service.name())
        }
    }

    /** Full drift-correction pass: online proxies vs. what's actually on Cloudflare right now. */
    private fun reconcile() {
        val cf = client ?: return
        if (!node.isHead()) return

        val cfg = config.value
        val online = onlineProxyServices()
        val desired = online.associate { commentFor(it) to it.host }
        val managed = cf.listRecords(cfg.record).filter { it.comment?.startsWith(MANAGED_PREFIX) == true }

        val toDelete = CloudflareSync.recordsToDelete(managed, desired.keys)
        toDelete.forEach { record -> record.id?.let(cf::deleteRecord) }

        val toCreate = CloudflareSync.entriesToCreate(managed, desired)
        toCreate.forEach { (comment, host) ->
            cf.createRecord(CloudflareRecord(name = cfg.record, content = host, ttl = cfg.ttl, proxied = cfg.proxied, comment = comment))
        }

        if (toDelete.isNotEmpty() || toCreate.isNotEmpty()) {
            logger.info("Cloudflare reconciled: +{} -{} record(s)", toCreate.size, toDelete.size)
        }
    }

    private fun onlineProxyServices(): List<Service> {
        val proxyGroups = Polocloud.groupService.find(GroupFilterType.PROXY).map { it.name }.toSet()
        return Polocloud.serviceService.findByState(ServiceState.RUNNING).filter { it.group in proxyGroups }
    }

    private fun isProxy(service: Service): Boolean =
        Polocloud.groupService.find(GroupFilterType.PROXY).any { it.name == service.group }

    private fun commentFor(service: Service): String = "$MANAGED_PREFIX${service.name()}"

    private companion object {
        const val MANAGED_PREFIX = "polocloud:"
    }
}
