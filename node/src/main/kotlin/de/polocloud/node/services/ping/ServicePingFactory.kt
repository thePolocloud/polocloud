package de.polocloud.node.services.ping

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceEventMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.ServiceResourceSampler
import de.polocloud.shared.service.ServiceState
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStartedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class ServicePingFactory(private val serviceProvider: ServiceProvider) {

    private val logger = LoggerFactory.getLogger(ServicePingFactory::class.java)
    private lateinit var thread: Thread
    private val resourceSampler = ServiceResourceSampler()

    fun run() {
        thread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tick()
                    Thread.sleep(POLL_INTERVAL_MILLIS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error("Service ping tick failed", e)
                }
            }
        }, "service-ping")
        thread.isDaemon = true
        thread.start()
        logger.info("Service ping factory started")
    }

    fun close() {
        if (this::thread.isInitialized) thread.interrupt()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val services = serviceProvider.localServices.filter { it.process?.isAlive == true }
        // Drop retained CPU-sampling snapshots for anything that isn't alive anymore, so a
        // stopped service's PID doesn't linger in the sampler for the rest of the node's uptime.
        resourceSampler.retainOnly(services.mapNotNull { it.process?.pid() }.toSet())
        if (services.isEmpty()) return

        runBlocking {
            services.map { service ->
                async(PING_DISPATCHER) { tickOne(service, now) }
            }.awaitAll()
        }
    }

    private fun tickOne(service: LocalService, now: Long) {
        // Refreshed here (not just on demand) because a process's descendants can
        // only be enumerated while it's still alive — if we waited until it crashed
        // to look, it would already be too late. See LocalService.lastKnownDescendants.
        service.sampleDescendants()
        sampleResourceUsage(service)
        if (service.port <= 0) return

        when {
            isAwaitingOnline(service) -> pingStarting(service)
            service.state == ServiceState.RUNNING -> pingPlayerCount(service, now)
        }
    }

    private fun sampleResourceUsage(service: LocalService) {
        val pid = service.process?.pid() ?: return
        val usage = resourceSampler.sample(pid) ?: return
        service.cpuUsage = usage.cpuUsage
        service.usedMemory = usage.usedMemory
    }

    // Pinged over service.hostname (== general.serviceHostname), not a hardcoded loopback:
    // a service only ever *listens* on the address it was told to bind to (e.g. Velocity's
    // `bind` is derived from the same value — see task_velocity_config.json in
    // polocloud-platforms), which for a VPN-only or multi-host deployment can be a specific
    // non-loopback interface. Pinging loopback unconditionally would never reach such a
    // service even though it is healthy. Defaults to 127.0.0.1 for a single-host setup, so
    // behavior there is unchanged.

    private fun pingStarting(service: LocalService) {
        val result = MinecraftServerPing.ping(service.hostname, service.port) ?: return
        markOnline(service, result)
    }

    private fun pingPlayerCount(service: LocalService, now: Long) {
        if (now - service.lastPlayerPollAt < PLAYER_POLL_INTERVAL_MILLIS) return
        service.lastPlayerPollAt = now

        val result = MinecraftServerPing.ping(service.hostname, service.port) ?: return
        val changed = service.onlinePlayers != result.onlinePlayers || service.maxPlayers != result.maxPlayers
        service.onlinePlayers = result.onlinePlayers
        service.maxPlayers = result.maxPlayers
        service.motd = result.description

        if (changed) {
            ClusterEventService.call(PlayerCountChangedEvent(ServiceEventMapper.toShared(service)))
        }
    }

    /** True while a service has been started but has not yet been confirmed online. */
    private fun isAwaitingOnline(service: LocalService): Boolean =
        service.state == ServiceState.STARTING || service.state == ServiceState.QUEUED

    private fun markOnline(service: LocalService, result: MinecraftPingResult) {
        service.state = ServiceState.RUNNING
        service.onlinePlayers = result.onlinePlayers
        service.maxPlayers = result.maxPlayers
        service.motd = result.description
        service.lastPlayerPollAt = System.currentTimeMillis()
        // Persist the RUNNING transition so the database no longer shows the service as
        // STARTING once it is actually online.
        serviceProvider.persist(service)
        logger.info(
            "Service {} is ONLINE — {} (protocol {}), {}/{} players, {}ms",
            service.name(), result.versionName, result.protocol,
            result.onlinePlayers, result.maxPlayers, result.latencyMillis,
        )

        // Re-broadcast the started event now that the service is reachable, so
        // subscribers see it with its confirmed RUNNING state.
        ClusterEventService.call(ServerStartedEvent(ServiceEventMapper.toShared(service)))
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1000L

        /** How often an already-`RUNNING` service is re-pinged to refresh its player count. */
        const val PLAYER_POLL_INTERVAL_MILLIS = 5000L

        /** Bounds how many pings run at once — plenty for cheap loopback socket calls. */
        val PING_DISPATCHER = Dispatchers.IO.limitedParallelism(16)
    }
}