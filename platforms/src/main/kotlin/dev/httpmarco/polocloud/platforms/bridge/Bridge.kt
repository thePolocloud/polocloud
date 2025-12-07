package dev.httpmarco.polocloud.platforms.bridge

import dev.httpmarco.polocloud.common.json.GSON
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists

const val groupId = "dev.httpmarco.polocloud"
const val repoUrl = "https://central.sonatype.com/repository/maven-snapshots/"

data class Bridge(val id: String, val version: String) {

    var type: BridgeType? = null
    var bridgeClass: String? = null

    fun isDownloaded(): Boolean {
        return path().exists()
    }

    fun download(): Boolean {
        this.downloadLatestSnapshotJar(groupId, id, version, repoUrl, path())
        return true
    }

    fun type(): BridgeType {
        if (bridgeClass == null) {
            updateContext()
        }
        return type!!
    }

    fun bridgeClass(): String {
        if (bridgeClass == null) {
            updateContext()
        }
        return bridgeClass!!
    }

    fun path(): Path {
        return Path.of("local/libs/$id-$version.jar")
    }

    fun updateContext() {
        JarFile(path().toFile()).use { jar ->
            val entry = jar.getJarEntry("bridge.json") ?: return
            jar.getInputStream(entry).use { input ->
                val json = input.readBytes().toString(Charsets.UTF_8)

                val config = GSON.fromJson(json, BridgeConfig::class.java)

                this.type = config.bridgeType
                this.bridgeClass = config.className
            }
        }
    }

    fun downloadLatestSnapshotJar(
        groupId: String,
        artifactId: String,
        version: String,
        repoUrl: String,
        target: Path
    ): Boolean {
        try {
            val groupPath = groupId.replace('.', '/')
            val base = "$repoUrl/$groupPath/$artifactId/$version"

            // Fetch metadata
            val metadataUrl = URL("$base/maven-metadata.xml")
            val xml = metadataUrl.openStream()

            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xml)

            val snapshot = doc.getElementsByTagName("snapshot").item(0)
            val timestamp = snapshot.childNodes.item(1).textContent
            val buildNumber = snapshot.childNodes.item(3).textContent

            // Remove -SNAPSHOT from version for final file name
            val baseVersion = version.removeSuffix("-SNAPSHOT")

            // Example: artifactId-3.0.0-pre.8-20251207.194905-2.jar
            val snapshotVersion = "$baseVersion-$timestamp-$buildNumber"
            val jarName = "$artifactId-$snapshotVersion.jar"

            val jarUrl = URL("$base/$jarName")

            jarUrl.openStream().use { input ->
                Files.copy(input, target)
            }

            return true

        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

}