package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.Runtime
import java.nio.file.Files
import java.nio.file.Paths


class DockerRuntime : Runtime {

    private val serviceStorage = DockerRuntimeServiceStorage()
    private val groupStorage = DockerRuntimeGroupStorage()

    override fun runnable(): Boolean {
        return try {
            return Files.exists(Paths.get("/.dockerenv")) || Files.exists(Paths.get("/run/.containerenv"));
        } catch (e: Exception) {
            logger.debug("Docker daemon not available: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }

    override fun serviceStorage() = serviceStorage

    override fun groupStorage() = groupStorage

}