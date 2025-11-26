package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractRuntimeFactory
import dev.httpmarco.polocloud.common.os.cpuUsage
import dev.httpmarco.polocloud.v1.services.ServiceState
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

/**
 * LocalRuntimeFactory manages service instances running locally on the host machine.
 * It handles service boot, shutdown, runtime tracking, and cleanup.
 */
class LocalRuntimeFactory(var localRuntime: LocalRuntime) : AbstractRuntimeFactory<LocalService>(LOCAL_FACTORY_PATH) {

    init {
        // If the local factory path exists, delete all contents to ensure a clean start
        if (LOCAL_FACTORY_PATH.exists()) {
            LOCAL_FACTORY_PATH.toFile().listFiles()?.forEach { it.deleteRecursively() }
        }
        // Create the factory directory if it doesn't exist
        LOCAL_FACTORY_PATH.createDirectories()
    }

    /**
     * Generate a new instance of a local service for a given group.
     */
    override fun generateInstance(group: AbstractGroup): LocalService {
        return LocalService(group)
    }

    /**
     * Shutdown any background threads or thread pools used by the factory.
     */
    fun shutdown() {
        cacheThreadPool.shutdown()
    }

    /**
     * Boot a local service process with appropriate runtime checks and environment.
     * Waits until CPU usage and concurrent starts are under configured limits.
     */
    override fun runRuntimeBoot(service: LocalService) {
        val version = service.group().platform.version
        val platform = service.group().platform()
        val versionObject = platform.version(version)

        // Check that the service runtime matches required version
        val (correctRuntime, currentRuntime) = checkRuntimeVersion(service)
        if (!correctRuntime) {
            if (currentRuntime == null) {
                i18n.warn(
                    "agent.local-runtime.factory.boot.missing-runtime",
                    service.group().platform().language,
                    versionObject?.requiredRuntimeVersion
                )
            } else {
                i18n.warn(
                    "agent.local-runtime.factory.boot.wrong-runtime",
                    currentRuntime,
                    service.group().platform().language,
                    versionObject?.requiredRuntimeVersion
                )
            }
        }

        // Wait until CPU usage and max concurrent starts allow launching the service
        while (Agent.runtime.serviceStorage().findAll()
                .count { it.state == ServiceState.STARTING } >= Agent.config.maxConcurrentServersStarts
            || cpuUsage() > Agent.config.maxCPUPercentageToStart
        ) {
            Thread.sleep(1000)
        }

        // Build and start the process
        val processBuilder = ProcessBuilder(languageSpecificBootArguments(service))
            .directory(service.path.toFile())

        // Inject environment variables into the service process
        processBuilder.environment().putAll(
            mapOf(
                "agent_port" to Agent.config.port.toString(),
                "service-name" to service.name()
            )
        )

        // Start the service process and begin tracking
        service.process = processBuilder.start()
        service.startTracking()
    }

    /**
     * Shutdown a local service safely.
     * Cleans up processes, stops tracking, handles recordings, and deletes non-static files.
     */
    @OptIn(ExperimentalPathApi::class)
    override fun runRuntimeShutdown(service: LocalService, shutdownCleanUp: Boolean) {
        // Attempt to gracefully shutdown the process
        service.process?.let { process ->
            try {
                val shutdownCommand = service.group().platform().shutdownCommand
                if (shutdownCommand.isNotEmpty() && shutdownCleanUp && service.executeCommand(shutdownCommand)) {
                    // Wait a short time for graceful exit
                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        service.state = ServiceState.STOPPED
                    }
                }
            } catch (_: Exception) {
                // Ignore exceptions, we only care about stopping the process
            }

            // Force-stop any remaining processes if still running
            if (service.state != ServiceState.STOPPED) {
                process.toHandle().children().forEach { child ->
                    try { child.destroy() } catch (_: Exception) { /* ignore */ }
                }
                process.toHandle().destroyForcibly()
                process.waitFor()
                service.process = null
                service.state = ServiceState.STOPPED
            }
        }

        // Stop screen recording if active
        if (localRuntime.terminal.screenService.isServiceRecoding(service)) {
            localRuntime.terminal.screenService.stopCurrentRecording()
        }

        // Stop internal service tracking
        service.stopTracking()

        // Give Windows a small delay to ensure process termination
        if (!Thread.currentThread().isVirtual && shutdownCleanUp) {
            Thread.sleep(200)
        }

        // Delete service files if not static
        if (!service.isStatic()) {
            service.path.deleteRecursively()
        }
    }
}
