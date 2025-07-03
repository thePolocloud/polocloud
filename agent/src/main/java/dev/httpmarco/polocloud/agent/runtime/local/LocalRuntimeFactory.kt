package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeFactory
import dev.httpmarco.polocloud.agent.services.Service
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class LocalRuntimeFactory : RuntimeFactory<LocalService> {

    private val factoryPath = Path("temp")

    init {
        // if folder exists, delete all files inside
        if (factoryPath.exists()) {
            factoryPath.toFile().listFiles()?.forEach { it.deleteRecursively() }
        }
        // init factory path
        factoryPath.createDirectories()
    }

    override fun bootApplication(service: LocalService) {

        if (service.state != Service.State.PREPARING) {
            logger.error("Cannot boot application for service ${service.name()} because it is not in PREPARING state, but in ${service.state} state&8. &7Wait for action&8...")
            return
        }

        logger.info("The service &3${service.name()}&7 is now starting&8...")

        val platform = service.group.platform()

        service.state = Service.State.STARTING
        service.path.createDirectories()

        val applicationPath = service.group.applicationPlatformFile()

        // download and copy the platform files to the service path
        platform.prepare(service.group.data.platform.version)
        Files.copy(applicationPath, service.path.resolve(applicationPath.name), StandardCopyOption.REPLACE_EXISTING)

        //todo remove: only for testing
        Files.writeString(service.path.resolve("eula.txt"), "eula=true")

        // basically current only the java command is supported yet
        val commands = ArrayList<String>()
        val javaPath = System.getProperty("java.home")

        commands.add("${javaPath}/bin/java")
        commands.addAll(
            listOf(
                "-Dterminal.jline=false",
                "-Dfile.encoding=UTF-8",
                "-Xms" + service.group.data.minMemory + "M",
                "-Xmx" + service.group.data.maxMemory + "M",
                "-jar",
                applicationPath.name
            )
        )
        commands.addAll(platform.arguments)

        val processBuilder = ProcessBuilder(commands).directory(service.path.toFile())

        // todo: env here set

        service.process = processBuilder.start()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun shutdownApplication(service: LocalService) {
        if (service.state == Service.State.STOPPING || service.state == Service.State.STOPPING) {
            logger.info("Cannot shutdown service ${service.name()} because it is already stopping or stopped&8. &7Wait for action&8...")
            return
        }

        logger.info("The service &3${service.name()}&7 is now stopping&8...")

        if (service.process != null) {
            service.executeCommand(service.group.platform().shutdownCommand)

            if (service.process!!.waitFor(5, TimeUnit.SECONDS)) {
                service.process!!.exitValue()
            }

            service.process!!.toHandle().destroyForcibly()
            service.process = null
        }

        Thread.sleep(200) // wait for a process to be destroyed
        service.path.deleteRecursively()

        service.state = Service.State.STOPPED
        Agent.instance.runtime.serviceStorage().dropService(service)
        logger.info("The service &3${service.name()}&7 has been stopped and deleted&8.")
    }
}