package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.RuntimeConfigHolder
import dev.httpmarco.polocloud.agent.runtime.RuntimeTemplates
import dev.httpmarco.polocloud.agent.services.Service
import java.nio.file.Files
import java.nio.file.Paths


class DockerRuntime : Runtime {

    private val serviceStorage = DockerRuntimeServiceStorage()
    private val groupStorage = DockerRuntimeGroupStorage()
    private val expender = DockerExpender()
    private val runtimeFactory = DockerFactory()

    override fun runnable(): Boolean {
        return try {
            return Files.exists(Paths.get("/.dockerenv")) || Files.exists(Paths.get("/run/.containerenv"))
        } catch (e: Exception) {
            i18n.debug("agent.runtime.docker.connection.failed", e.javaClass.simpleName, e.message)
            false
        }
    }

    override fun serviceStorage() = serviceStorage

    override fun groupStorage() = groupStorage

    override fun factory() = runtimeFactory

    override fun expender() = expender

    override fun templates(): RuntimeTemplates<Service> {
        TODO("Not yet implemented")
    }

    override fun configHolder(): RuntimeConfigHolder {
        TODO("Not yet implemented")
    }

}