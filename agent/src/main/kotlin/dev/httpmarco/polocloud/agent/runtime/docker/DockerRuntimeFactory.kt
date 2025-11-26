package dev.httpmarco.polocloud.agent.runtime.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.*
import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.runtime.abstract.AbstractRuntimeFactory
import dev.httpmarco.polocloud.v1.groups.GroupType
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path


/**
 * DockerRuntimeFactory is responsible for managing Docker-based service instances.
 * It can start, stop, and generate Docker services dynamically using Docker Swarm.
 * This class is Swarm-aware: it creates services instead of plain containers, allowing
 * replication and overlay networking.
 */
class DockerRuntimeFactory(val client: DockerClient) : AbstractRuntimeFactory<DockerService>(Path("local/temp")) {

    /**
     * Starts a Docker service using Docker Swarm.
     * Pulls the image, sets up mounts, networking, and service replication.
     */
    override fun runRuntimeBoot(service: DockerService) {
        val createCmd = client.createContainerCmd("openjdk:21-jdk")
            .withName(service.name())
            .withWorkingDir("/app")
            .withCmd(*languageSpecificBootArguments(service).toTypedArray())

        val envList = environment(service).parameters.map { (key, value) -> "$key=$value" }.toMutableList()

        // TODO USE RIGHT ENV FOR USE 100% OF RAM
        //envList.add("MEMORY=")
        if (envList.isNotEmpty()) {
            createCmd.withEnv(*envList.toTypedArray())
        }

        val bindSource = localUserVolumePath() + "\\temp\\${service.name()}"
        val hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(true)
            .withBinds(Bind.parse("$bindSource:/app"))
            .withMemory(service.maxMemory * 1024 * 1024L)

        if (service.type == GroupType.PROXY) {
            val exposed = ExposedPort.tcp(service.port)
            createCmd.withExposedPorts(exposed)
            hostConfig.withPortBindings(PortBinding(Ports.Binding.bindPort(service.port), exposed))
        }

        createCmd.withHostConfig(hostConfig)

        val containerResponse = createCmd.exec()
        client.startContainerCmd(containerResponse.id).exec()

        val inspection = client.inspectContainerCmd(containerResponse.id).exec()
        val ip = inspection.networkSettings.networks.values.firstOrNull()?.ipAddress
            ?: throw IllegalStateException("null")

        service.containerId = containerResponse.id
        service.changeToContainerHostname(ip)
    }

    /**
     * Stops and removes the Docker service.
     * Uses the service container ID, forces removal if needed.
     */
    override fun runRuntimeShutdown(service: DockerService, shutdownCleanUp: Boolean) {
        val id = service.containerId ?: return
        try {
            try {
                client.stopContainerCmd(id).exec()
                client.waitContainerCmd(id).start().awaitStatusCode()
            } catch (_: NotModifiedException) {
                // Container was already stopped
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun javaLanguagePath(service: DockerService): List<String> {
        return listOf("java")
    }

    /**
     * Generates a new DockerService instance for a given group.
     */
    override fun generateInstance(group: AbstractGroup): DockerService {
        return DockerService(group)
    }

    private fun localUserVolumePath(): String {
        val string = containerId()
        val containerInfo = client.inspectContainerCmd(string).exec()
        val mounts = containerInfo.mounts

        val targetPath = "/cloud/local"

        val mount = mounts!!.firstOrNull { it.destination!!.path == targetPath }
            ?: throw IllegalStateException("No mount found for $targetPath")

        return mount.source!!
    }

    fun containerId(): String {
        // 1) try env HOSTNAME
        System.getenv("HOSTNAME")?.let {
            if (it.matches(Regex("[0-9a-f]{12,64}"))) return it
        }

        // 2) try etc/hostname
        try {
            val host = Files.readString(Paths.get("/etc/hostname")).trim()
            if (host.matches(Regex("[0-9a-f]{12,64}"))) return host
        } catch (_: Exception) {
        }

        // 3) (Optional) try /proc/self/mountinfo
        try {
            val lines = Files.readAllLines(Paths.get("/proc/self/mountinfo"))
            val regex = Regex("[0-9a-f]{64}")
            lines.forEach { line ->
                regex.find(line)?.let { return it.value }
            }
        } catch (_: Exception) {
        }

        // nichts gefunden
        return ""
    }
}
