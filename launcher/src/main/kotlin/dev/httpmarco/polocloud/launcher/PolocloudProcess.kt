package dev.httpmarco.polocloud.launcher

import java.util.*

class PolocloudProcess : Thread() {

    private val processLibs: List<PolocloudLib>

    init {
        name = "PolocloudProcess"
        isDaemon = false

        this.processLibs = PolocloudLib.of(*REQUIRED_LIBS)
        this.processLibs.forEach(PolocloudLib::copyFromClasspath)
    }

    override fun run() {
        val processBuilder = ProcessBuilder()
            .inheritIO()
            .command("java", "-jar", "polocloud.jar")

        // copy all environment variables from the current process
        processBuilder.environment().putAll(System.getenv())

        val process = processBuilder.start()

        // wait for the process to finish
        process.waitFor()
        process.exitValue()
    }

    private fun arguments(): List<String> {
        val arguments = ArrayList<String>()
        val usedJava = System.getenv("java.home")

        val bootLib = this.processLibs.stream().filter { it.name == BOOT_LIB }.findFirst().orElseThrow { LibNotFoundException(BOOT_LIB) }

        arguments.add("$usedJava/bin/java")
        arguments.add("-javaagent:%s".format(bootLib.target()))

        arguments.add("-cp")
        arguments.add(
            java.lang.String.join(
                if (windowsProcess()) ";" else ":",
                processLibs.stream().map({ it -> it.target().toString() }).toList()
            )
        )
        arguments.add(bootLib.mainClass())
        return arguments
    }

    private fun windowsProcess(): Boolean {
        return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
    }
}