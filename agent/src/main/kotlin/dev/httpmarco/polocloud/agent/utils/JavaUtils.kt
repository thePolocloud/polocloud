package dev.httpmarco.polocloud.agent.utils

import java.io.File
import java.lang.ProcessBuilder
import org.slf4j.LoggerFactory

class JavaUtils {

    fun isValidJavaPath(path: String): Boolean {
        val trimmedPath = path.trim()
        logger.debug("Validating Java path: '$trimmedPath'")

        val dir = File(trimmedPath)

        if (!dir.exists()) {
            logger.debug("Directory does not exist: ${dir.absolutePath}")
            return false
        }

        if (!dir.isDirectory) {
            logger.debug("Path is not a directory: ${dir.absolutePath}")
            return false
        }

        val javaExe = if (isWindows()) {
            File(dir, "bin/java.exe")
        } else {
            File(dir, "bin/java")
        }

        logger.debug("Checking Java executable: ${javaExe.absolutePath}")
        logger.debug("Exists: ${javaExe.exists()}")
        logger.debug("Can execute: ${javaExe.canExecute()}")

        if (!javaExe.exists()) {
            logger.debug("Java executable not found")
            return false
        }

        return testJavaExecution(javaExe.absolutePath)
    }

    private fun testJavaExecution(javaPath: String): Boolean {
        return try {
            val command = if (isWindows() && javaPath.contains(" ")) {
                "\"$javaPath\" -version"
            } else {
                "$javaPath -version"
            }

            val process = if (isWindows()) {
                ProcessBuilder("cmd", "/c", command)
            } else {
                ProcessBuilder("sh", "-c", command)
            }

            process.redirectErrorStream(true)
            val p = process.start()

            val exitCode = p.waitFor()
            val output = p.inputStream.bufferedReader().readText()

            val isValid = exitCode == 0 && output.contains("version", ignoreCase = true)

            if (!isValid) {
                logger.debug("Output: $output")
            }

            isValid
        } catch (e: Exception) {
            logger.debug("Java execution test failed: ${e.message}")
            false
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").contains("win", ignoreCase = true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JavaUtils::class.java)
    }
}