package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeFactory
import dev.httpmarco.polocloud.agent.services.Service
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.name

class LocalRuntimeFactory : RuntimeFactory {

    private val factoryPath = Path("temp")

    init {
        // if folder exists, delete all files inside
        if (factoryPath.exists()) {
            factoryPath.toFile().listFiles()?.forEach { it.deleteRecursively() }
        }
        // init factory path
        factoryPath.createDirectories()
    }

    override fun bootApplication(service: Service) {

        if (service.state != Service.State.PREPARING) {
            logger.error("Cannot boot application for service ${service.name()} because it is not in PREPARING state, but in ${service.state} state&8. &7Wait for action&8...")
            return
        }

        logger.info("The service &3${service.name()}&7 is now starting&8...")

        val platform = service.group.platform()

        if (platform == null) {
            logger.error("Cannot boot service ${service.name()} because the platform is null. Make sure the group has a valid platform assigned.")
            return
        }

        service.state = Service.State.STARTING
        service.path.createDirectories()

        val applicationPath = service.group.applicationPlatformFile()

        // download and copy the platform files to the service path
        platform.prepare(service.group.data.platform.version)
        Files.copy(applicationPath, service.path.resolve(applicationPath.name), StandardCopyOption.REPLACE_EXISTING)

        // basically current only the java command is supported yet
        val commands = listOf("java", "-jar", applicationPath.name)

        service.process = ProcessBuilder().directory(service.path.toFile()).command(commands).start()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun shutdownApplication(service: Service) {
        //todo shutdown with command in input stream

        if (service.state == Service.State.STOPPING || service.state == Service.State.STOPPING) {
            logger.info("Cannot shutdown service ${service.name()} because it is already stopping or stopped&8. &7Wait for action&8...")
            return
        }

        logger.info("The service &3${service.name()}&7 is now stopping&8...")
        //  if (service.process().waitFor(PolocloudSuite.instance().config().local().processTerminationIdleSeconds(), TimeUnit.SECONDS)) {

        if (service.process != null) {
            service.process!!.toHandle().destroyForcibly()
            service.process = null
        }

        Thread.sleep(200) // wait for a process to be destroyed
        service.path.deleteRecursively()

        Agent.instance.runtime.serviceStorage().dropService(service)
        logger.info("The service &3${service.name()}&7 has been stopped and deleted&8.")
    }
}