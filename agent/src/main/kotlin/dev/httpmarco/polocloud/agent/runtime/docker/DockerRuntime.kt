package dev.httpmarco.polocloud.agent.runtime.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import dev.httpmarco.polocloud.agent.runtime.*
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractServiceStatsThread
import dev.httpmarco.polocloud.common.network.localAddress
import dev.httpmarco.polocloud.v1.services.ServiceState

/**
 * DockerRuntime integrates PoloCloud with Docker, providing:
 * - Service and group management backed by Docker.
 * - Template handling.
 * - Runtime factory for creating new services.
 * - Automatic event listening for container lifecycle changes.
 *
 * It acts as the central runtime implementation for Docker-based environments.
 */
class DockerRuntime : Runtime() {

    private val client: DockerClient = createLocalDockerClient()
    private val serviceStorage = DockerRuntimeServiceStorage(client)
    private val groupStorage = DockerRuntimeGroupStorage()
    private val expender = DockerExpender(client)
    private val runtimeFactory = DockerRuntimeFactory(client)
    private val templateStorage = DockerTemplateStorage(client)
    private val dockerConfigHolder = DockerConfigHolder()
    private val informationThread = DockerCloudInformationThread()
    private val queue = DockerThreadedRuntimeQueue()

    /**
     * Creates a DockerClient that connects to the local Docker daemon.
     *
     * @return Configured DockerClient instance.
     */
    private fun createLocalDockerClient(): DockerClient {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

        val httpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .build()

        return DockerClientImpl.getInstance(config, httpClient)
    }

    /**
     * Bootstraps the runtime by:
     * - Registering an event listener for container stop events.
     * - Starting information and queue handling threads.
     */
    override fun boot() {
        listenForContainerEvents()
        informationThread.start()
        queue.start()
    }

    /**
     * Subscribes to Docker container events and handles PoloCloud-related container shutdowns.
     */
    private fun listenForContainerEvents() {
        client.eventsCmd().exec(object : ResultCallback.Adapter<Event>() {
            override fun onNext(event: Event) {
                try {
                    if (event.type == EventType.CONTAINER && event.action == "destroy") {
                        serviceStorage.findAll().stream().filter { it.containerId == event.id }.findFirst()
                            .ifPresent { service -> service.changeState(ServiceState.STOPPING) }
                        return
                    }

                    if (event.type == EventType.CONTAINER && event.action == "die") {
                        serviceStorage.findAll().stream().filter { it.containerId == event.id }.findFirst()
                            .ifPresent { service -> factory().shutdownApplication(service, true) }
                    }
                } catch (e: Exception) {
                    System.err.println("Error while handling Docker event: ${e.message}")
                    e.printStackTrace()
                }
            }
        })
    }

    override fun serviceStorage() = serviceStorage

    override fun groupStorage() = groupStorage

    override fun factory() = runtimeFactory

    override fun expender() = expender

    override fun templateStorage() = templateStorage

    override fun configHolder() = dockerConfigHolder

    override fun sendCommand(command: String) {
        // TODO: Implement command sending logic if required
    }

    /**
     * Detects the first non-loopback IPv4 address of the host machine.
     *
     * @return Local IPv4 address as String, or "null" if none was found.
     */
    override fun detectLocalAddress(): String {
        return localAddress()
    }

    /**
     * Provides the statistics thread for monitoring container CPU and memory usage.
     */
    override fun serviceStatsThread(): AbstractServiceStatsThread<*> {
        return DockerServiceStatsThread(client)
    }
}
