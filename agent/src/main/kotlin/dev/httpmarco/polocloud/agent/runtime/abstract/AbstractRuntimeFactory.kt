package dev.httpmarco.polocloud.agent.runtime.abstract

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.RuntimeFactory
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.agent.utils.JavaUtils
import dev.httpmarco.polocloud.common.image.pngToBase64DataUrl
import dev.httpmarco.polocloud.common.language.Language
import dev.httpmarco.polocloud.common.os.currentOS
import dev.httpmarco.polocloud.common.version.polocloudVersion
import dev.httpmarco.polocloud.platforms.Platform
import dev.httpmarco.polocloud.platforms.PlatformParameters
import dev.httpmarco.polocloud.platforms.ServerPlatformForwarding
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangeStateEvent
import dev.httpmarco.polocloud.shared.properties.JAVA_PATH
import dev.httpmarco.polocloud.v1.services.ServiceSnapshot
import dev.httpmarco.polocloud.v1.services.ServiceState
import org.yaml.snakeyaml.util.Tuple
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.addAll
import kotlin.io.path.createDirectories
import kotlin.io.path.name

abstract class AbstractRuntimeFactory<T : AbstractService>(val factoryPath: Path) : RuntimeFactory<T> {

    val cacheThreadPool: ExecutorService by lazy { Executors.newFixedThreadPool(Agent.config.maxCachingProcesses) }
    val runningCacheProcesses: MutableList<Tuple<String, String>> by lazy {
        Collections.synchronizedList(
            mutableListOf()
        )
    }
    val waitingServices: MutableList<T> by lazy { Collections.synchronizedList(mutableListOf()) }

    /**
     * Boots the given [service].
     * If the service's platform version is not cached, it will be cached first.
     * If the platform is currently being cached, the service will be added to the waiting list
     * and booted as soon as the caching is done.
     */
    override fun bootApplication(service: T) {
        if (service.state != ServiceState.PREPARING) {
            i18n.error("agent.local-runtime.factory.boot.error", service.name(), service.state)
            return
        }

        val platform = service.group().platform()
        val version = service.group().platform.version

        val environment = this.environment(service)

        val path = service.path
        path.createDirectories()

        //loading cache before starting service
        val cacheIsRunning = runningCacheProcesses.any { platform.name == it._1() && version == it._2() }
        if (!platform.cacheExists(version) || cacheIsRunning) {
            waitingServices.add(service)

            if (!cacheIsRunning) {
                this.handleMissingCache(platform, version, environment)
            }
            return
        }

        i18n.info("agent.local-runtime.factory.boot.up", service.name())
        service.state = ServiceState.STARTING
        Agent.eventService.call(ServiceChangeStateEvent(service))

        // copy all templates to the service path
        Agent.runtime.templateStorage().bindTemplate(service)

        // copy the platform files to the service path and setup service
        platform.prepare(path, service.group().platform.version, environment)

        val serverIcon = this.javaClass.classLoader.getResource("server-icon.png")!!
        val serverIconPath = path.resolve("server-icon.png")
        // copy server-icon if not exists
        if (Files.notExists(serverIconPath)) {
            Files.copy(serverIcon.openStream(), serverIconPath)
        }

        this.runRuntimeBoot(service)
    }

    override fun shutdownApplication(service: T, shutdownCleanUp: Boolean): ServiceSnapshot {
        // If the service is already stopping or stopped, return its snapshot
        if (service.state == ServiceState.STOPPING || service.state == ServiceState.STOPPED) {
            return service.toSnapshot()
        }

        service.state = ServiceState.STOPPING
        val eventService = Agent.eventService

        i18n.info("agent.local-runtime.factory.shutdown", service.name())

        // Remove any event subscriptions for this service
        eventService.dropServiceSubscriptions(service)
        // Notify other services that this service is stopping
        eventService.call(ServiceChangeStateEvent(service))

        this.runRuntimeShutdown(service, shutdownCleanUp)

        // Finalize service state and fire shutdown event
        service.state = ServiceState.STOPPED
        Agent.eventProvider().call(ServiceChangeStateEvent(service))
        Agent.runtime.serviceStorage().dropAbstractService(service)

        i18n.info(
            "agent.local-runtime.factory${if (service.isStatic()) ".static" else ""}.shutdown.successful",
            service.name()
        )
        return service.toSnapshot()
    }

    /**
     * Runs the runtime boot process for the given [service].
     * This method is called after all checks are done and the environment is prepared.
     */
    abstract fun runRuntimeBoot(service: T)

    abstract fun runRuntimeShutdown(service: T, shutdownCleanUp: Boolean)

    /**
     * Prepares the environment parameters for the given [service].
     * This includes parameters like hostname, port, server icon, agent port, service name,
     * proxy token, file suffix, and platform file name.
     *
     * @param service The service for which to prepare the environment.
     * @return A [PlatformParameters] object containing all necessary parameters.
     * @see PlatformParameters
     */
    protected fun environment(service: T) : PlatformParameters {
        val version = service.group().platform.version
        val platform = service.group().platform()
        val versionObject = platform.version(version)

        val environment = PlatformParameters(
            versionObject
        )

        val serverIcon = this.javaClass.classLoader.getResource("server-icon.png")!!

        environment.addParameter("hostname", service.hostname)
        environment.addParameter("port", service.port)
        environment.addParameter("server_icon", pngToBase64DataUrl(serverIcon.openStream()))
        environment.addParameter("agent_port", Agent.config.port)
        environment.addParameter("agent_hostname", Agent.runtime.detectLocalAddress())
        environment.addParameter("service-name", service.name())
        environment.addParameter("velocityProxyToken", Agent.securityProvider.proxySecureToken)
        environment.addParameter("file_suffix", platform.language.suffix())
        environment.addParameter("filename", service.group().applicationPlatformFile().name)

        val velocityPlatforms = listOf("velocity", "gate")

        val groupStorage = Agent.runtime.groupStorage()
        val modernForwardingMode = groupStorage.findAll().filter { it.isServer() }.all { it.platform().forwarding == ServerPlatformForwarding.MODERN }

        environment.addParameter("forwarding", (if(modernForwardingMode) ServerPlatformForwarding.MODERN.name else ServerPlatformForwarding.LEGACY.name).lowercase())

        // TODO USE THIS WITH ALL PLATFORMS
        environment.addParameter("velocity_use", groupStorage.findAll().stream().anyMatch { velocityPlatforms.contains(it.platform().name) })

        System.err.println(groupStorage.findAll().stream().anyMatch { it.platform().name == "bungeecord" })
        environment.addParameter("bungeecord_use", groupStorage.findAll().stream().anyMatch { it.platform().name == "bungeecord" })
        environment.addParameter("version", polocloudVersion())

        return environment
    }

    protected fun handleMissingCache(platform: Platform, version: String, environment: PlatformParameters) {
        val platformName = platform.name

        val processEntry = Tuple(platformName, version)
        runningCacheProcesses.add(processEntry)

        cacheThreadPool.execute {
            i18n.info("agent.local-runtime.factory.boot.platform.prepare", version, platformName)
            platform.cachePrepare(version, environment)
            runningCacheProcesses.remove(processEntry)

            val servicesToBoot =
                waitingServices.filter { it.group().platform.name == platform.name && it.group().platform.version == version }
            servicesToBoot.forEach {
                this.bootApplication(it)
            }
            waitingServices.removeAll(servicesToBoot)
        }
    }

    protected fun languageSpecificBootArguments(service: T): ArrayList<String> {
        val platform = service.group().platform()
        val commands = ArrayList<String>()

        when (platform.language) {
            Language.JAVA -> {
                commands.addAll(javaLanguagePath(service))
                commands.addAll(
                    listOf(
                        "-Dterminal.jline=false",
                        "-Dfile.encoding=UTF-8",
                        "-Xms" + service.minMemory + "M",
                        "-Xmx" + service.maxMemory + "M"
                    )
                )

                if (platform.flags.isNotEmpty()) {
                    commands.addAll(platform.flags)
                }

                commands.addAll(
                    listOf(
                        "-jar",
                        service.group().applicationPlatformFile().name
                    )
                )
                commands.addAll(platform.arguments)
            }

            Language.GO, Language.RUST -> {
                commands.addAll(currentOS.executableCurrentDirectoryCommand(service.group().applicationPlatformFile().name))
            }
        }
        return commands
    }

    protected open fun javaLanguagePath(service: T) : List<String> {
        val javaPath = service.group().properties.get(JAVA_PATH)?.takeIf {
            JavaUtils().isValidJavaPath(it)
        } ?: System.getProperty("java.home")
        return listOf("${javaPath}/bin/java")
    }
}