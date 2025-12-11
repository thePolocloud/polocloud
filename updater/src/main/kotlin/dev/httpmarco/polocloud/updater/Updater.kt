package dev.httpmarco.polocloud.updater

import dev.httpmarco.polocloud.common.os.OS
import dev.httpmarco.polocloud.common.os.currentOS
import dev.httpmarco.polocloud.common.version.polocloudVersion
import java.io.File
import java.util.LinkedList

object Updater {

    private var syncGitHubVersion = false
    private val versions = LinkedList<String>()

    init {
        this.tryUpdate()
    }

    fun latestVersion(): String {
        return versions.first()
    }

    fun newVersionAvailable(): Boolean {
        return polocloudVersion() != latestVersion() && versions.contains(polocloudVersion())
    }

    fun hasSyncGitHubVersion() = syncGitHubVersion

    fun availableVersions(): List<String> {
        return versions
    }

    fun update(version: String = latestVersion()) {
        println("Launching updater...")

        val jarName = "polocloud-updater-${polocloudVersion()}.jar"

        val processBuilder = when (currentOS) {
            OS.WIN -> ProcessBuilder(
                "cmd.exe",
                "/c",
                "start",
                "cmd.exe",
                "/c",
                "java",
                "-jar",
                jarName,
                "--version=$version"
            )

            else -> ProcessBuilder("java", "-jar", jarName, "--version=$version")
        }

        processBuilder.environment()["POLOCLOUD_VERSION"] = polocloudVersion()
        processBuilder.directory(File("local/libs"))
        processBuilder.inheritIO()

        processBuilder.start()
    }

    fun tryUpdate() {
        this.versions.clear()
        this.versions += readTags()

        if (!versions.isEmpty()) {
            syncGitHubVersion = true
        } else {
            versions += polocloudVersion()
        }
    }
}