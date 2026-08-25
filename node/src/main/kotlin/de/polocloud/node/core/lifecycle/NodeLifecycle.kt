package de.polocloud.node.core.lifecycle

import de.polocloud.api.connection.ServiceCertificateStorage
import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseAccess
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trError
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.bootstrap.time.StartupTimer
import de.polocloud.node.core.NodeRuntime
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.event.ClusterEventRelay
import de.polocloud.node.module.ClusterModuleRegistry
import de.polocloud.node.security.ServiceIdentityProvisioner
import de.polocloud.updater.UpdateChecker
import de.polocloud.updater.Updater
import org.apache.logging.log4j.LogManager
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class NodeLifecycle(
    val holder: ConfigurationHolder<NodeConfigurations>,
    private val runtime: NodeRuntime
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var context: NodeRuntimeContext
        private set

    // Forwards this node's events to peers while it is running (cluster-wide live events).
    private var eventRelay: ClusterEventRelay? = null

    fun initialize() {
        // Blocking and first thing on boot when enabled: applying (and relaunching for) an
        // update before anything else starts means a restart never drops live workload.
        // Never throws — see Updater.downloadAndRestartIfAvailable.
        if (holder.value.general.autoUpdate) {
            Updater.downloadAndRestartIfAvailable()
        }

        val props = runtime.launchProperties

        TranslationService.init()
        TranslationService.defaultLanguage(holder.value.general.locale)

        DatabaseAccess.initialize(holder.value.localNode.database)

        if (!DatabaseAccess.connect()) {
            throw IllegalStateException("Database connection failed")
        }

        context = runtime.identityService.resolve(props)

        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown(ShutdownMode.GRACEFUL)
        })
    }

    fun start() {
        val container = context.localNodeContainer

        if (!container.isStarting()) {
            throw IllegalStateException(
                "Node is not in starting state: ${container.state()}"
            )
        }

        context.registrationManager.allowRequests()

        container.markOnline()

        val timing = holder.value.cluster.timing
        runtime.heartBeatService.startScheduler(timing.heartbeatIntervalMillis.milliseconds)
        runtime.electionService.start(
            container.data.id,
            baseTimeout = timing.electionBaseTimeoutMillis.milliseconds,
            jitterRangeMillis = timing.electionJitterRangeMillis,
            heartbeatInterval = timing.leaderHeartbeatIntervalMillis.milliseconds,
        )
        runtime.heartBeatMonitor.start()
        runtime.nodePruneService.start()

        context.groupService.run()
        context.serviceProvider.run()

        provisionModuleIdentity()

        ClusterModuleRegistry.start()
        context.moduleManager.loadAll()

        // Start relaying local events to peers so subscribers on any node see the whole
        // cluster's service lifecycle live. No-op while this is the only node.
        eventRelay = ClusterEventRelay(context.serviceProvider.nodeId).also { it.install() }

        logger.trInfo(
            "cluster",
            "cluster.node.started",
            "version" to PolocloudVersion.CURRENT.toDisplayString(),
            "time" to StartupTimer.formatted
        )

        // Best-effort, never blocks boot — see UpdateChecker. Skipped when autoUpdate already
        // ran: it either just applied the latest release or confirmed we're on it.
        if (!holder.value.general.autoUpdate) {
            UpdateChecker.checkOnBootAsync()
        }

        context.cli.readingThread.start()
    }

    /**
     * Provisions an mTLS identity for the node's own process into the SDK's default
     * identity directory (see [ServiceCertificateStorage.defaultIdentityDir]), so
     * in-process modules calling [de.polocloud.api.Polocloud] (e.g. the status
     * module's REST API) can open a loopback connection back to this node the same
     * way a launched service does via [ServiceIdentityProvisioner] — without this,
     * every such call fails with "No provisioned service identity found", since that
     * provisioner was previously only ever invoked for spawned service processes,
     * never for the node's own JVM.
     *
     * Also pins the loopback host/port explicitly rather than relying on
     * [de.polocloud.api.connection.PolocloudConnection]'s defaults happening to match,
     * so this keeps working if `general.apiAddress` is ever configured to a non-default
     * port.
     */
    private fun provisionModuleIdentity() {
        ServiceIdentityProvisioner.provision(
            ServiceCertificateStorage.defaultIdentityDir(),
            serviceId = "node-${context.localNodeContainer.data.id}",
            planName = "node",
        )
        System.setProperty("polocloud.node.host", "127.0.0.1")
        System.setProperty("polocloud.node.port", holder.value.general.apiAddress.port.toString())
    }

    fun shutdown(mode: ShutdownMode) {
        val container = context.localNodeContainer

        if (container.isOffline() || container.inShutdownProcess()) {
            return
        }

        logger.trInfo("node", "node.shutdown.stopping")
        container.markStopping()

        safe("eventRelay") {
            eventRelay?.close()
        }

        safe("moduleManager") {
            context.moduleManager.unloadAll()
            ClusterModuleRegistry.stop()
        }

        safe("heartBeatMonitor") {
            runtime.heartBeatMonitor.stop()
        }

        safe("electionService") {
            runtime.electionService.onHeadNodeLeft()
            runtime.electionService.stop()
        }

        safe("nodePruneService") {
            runtime.nodePruneService.stop()
        }

        safe("heartBeatService") {
            runtime.heartBeatService.stopScheduler()
        }


        safe("serviceProvider") {
            context.serviceProvider.shutdown()
        }

        safe("serviceGrpcEndpoint") {
            context.serviceGrpcEndpoint.close(mode)
        }

        // Also broadcasts this node's departure to peers and stops CLI session cleanup —
        // see NodeGrpcEndpoint.close. Previously never called, so the mTLS server socket
        // stayed bound, peers only learned this node left via heartbeat timeout instead of
        // immediately, and the Netty server threads leaked past process shutdown.
        safe("grpcEndpoint") {
            context.grpcEndpoint.close(mode)
        }

        safe("registrationManager") {
            context.registrationManager.close(mode)
        }

        safe("localNodeContainer") {
            container.markStopped()
        }

        safe("database") {
            DatabaseAccess.close()
        }

        logger.trInfo("node", "node.shutdown.stopped")
        LogManager.shutdown()
    }

    private fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            logger.trError("node", "node.shutdown.task.error", "task" to name)
        }
    }
}