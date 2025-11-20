package dev.httpmarco.polocloud.platforms

import dev.httpmarco.polocloud.common.filesystem.copyDirectory
import dev.httpmarco.polocloud.common.language.Language
import dev.httpmarco.polocloud.common.os.OS
import dev.httpmarco.polocloud.common.os.currentCPUArchitecture
import dev.httpmarco.polocloud.common.os.currentOS
import dev.httpmarco.polocloud.platforms.bridge.Bridge
import dev.httpmarco.polocloud.platforms.bridge.BridgeType
import dev.httpmarco.polocloud.platforms.exceptions.PlatformCacheMissingException
import dev.httpmarco.polocloud.platforms.exceptions.PlatformVersionInvalidException
import dev.httpmarco.polocloud.platforms.tasks.PlatformTask
import dev.httpmarco.polocloud.platforms.tasks.PlatformTaskPool
import dev.httpmarco.polocloud.v1.GroupType
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

class Platform(
    val name: String,
    val url: String,
    val language: Language,
    // default is "stop"
    val shutdownCommand: String = "stop",
    // the type of platform, proxy, server, or service
    val type: GroupType,
    // all global arguments for the platform after the jar name, for example, 'nogui'
    val arguments: List<String> = emptyList(),
    // all global flags for the platform after the jar name, for example: '-Djava.net.preferIPv4Stack=true'
    val flags: List<String> = emptyList(),
    // all versions of the platform that are supported
    val versions: List<PlatformVersion>,
    // if bridge present, the bridge that should be used for this platform
    val bridge: Bridge? = null,
    //The first port used for port selection unless startPort is set in group
    val defaultStartPort: Int?,
    // if the path is empty, the platform will not copy the bridge
    private val bridgePath: String? = null,
    // the tasks that should be run after the platform is prepared
    private val tasks: List<String> = emptyList(),
    // the tasks that should be run after the platform download
    private val preTasks: List<String> = emptyList(),
    // if true, the polocloud server icon will be copied to the service path
    private val copyServerIcon: Boolean = true,
    // if false, the downloaded file will be named "download" to be changed by preTask
    private val setFileName: Boolean = true,
    // mapping how the OS names will be named in the %os% placeholder (optional)
    private val osNameMapping: Map<OS, String> = emptyMap(),
    // mapping how the architecture names will be named in the %arch% placeholder (optional)
    private val archNameMapping: Map<String, String> = emptyMap(),
    // property for proxy forwarding support
    val forwarding: ServerPlatformForwarding = ServerPlatformForwarding.LEGACY
) {

    fun prepare(servicePath: Path, version: String, environment: PlatformParameters) {
        // This method should handle the preparation of the platform setting up the environment for the specified version.
        // Implementation details would depend on the specific requirements of the platform.
        val path = cachePath(version)
        val version = this.version(version)

        environment.addParameter("filename", path.fileName)

        if (version == null) {
            throw PlatformVersionInvalidException()
        }

        if (!path.exists()) {
            throw PlatformCacheMissingException()
        }

        tasks().forEach { it.runTask(servicePath, environment) }

        // copy the platform file to the service path
        copyDirectory(path.parent, servicePath)

        if (bridge == null) {
            return
        }

        //on_promise situation -> copy files to the service path
        if (bridge.type == BridgeType.ON_PREMISE) {
            val targetBridge = servicePath.resolve(bridgePath + "/" + bridge.path.name)
            targetBridge.parent.createDirectories()
            Files.copy(bridge.path, targetBridge, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        if (bridge.type == BridgeType.OFF_PREMISE) {
            val bridgeClass = Class.forName(bridge.bridgeClass)
            bridgeClass.getDeclaredConstructor(Path::class.java, String::class.java, Int::class.java, String::class.java)
                .newInstance(
                    servicePath,
                    environment.getStringParameter("service-name"),
                    environment.getIntParameter("agent_port"),
                    environment.getIntParameter("agent_hostname")
                )
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun cachePrepare(version: String, environment: PlatformParameters) {
        // This method should build the cache for a platform version, such as downloading files,
        // loading dependencies or compile the platform
        val cachePath = cachePath(version)
        cachePath.parent.createDirectories()

        try {

            val version = this.version(version) ?: throw PlatformVersionInvalidException()

            var replacedUrl = url.replace("%version%", version.version)
                .replace("%suffix%", language.suffix())
                .replace("%arch%", archDownloadName())
                .replace("%os%", osDownloadName())

            version.additionalProperties.forEach { (key, value) ->
                replacedUrl = replacedUrl.replace("%$key%", value.asJsonPrimitive.asString)
            }

            val downloadFile = if (setFileName) cachePath.toFile() else cachePath.parent.resolve("download").toFile()

            URI(
                replacedUrl
            ).toURL().openStream().use { input ->
                downloadFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            preTasks().forEach { it.runTask(cachePath.parent, environment) }

            if (language.nativeExecutable && downloadFile.exists() && listOf(OS.LINUX, OS.MACOS).contains(currentOS)) {
                val perms = downloadFile.toPath().getPosixFilePermissions().toMutableSet()
                if (!perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
                    perms.add(PosixFilePermission.OWNER_EXECUTE)
                    downloadFile.toPath().setPosixFilePermissions(perms)
                }
            }
        } catch (e: Exception) {
            cachePath.parent.deleteRecursively()
            throw e
        }
    }

    fun cacheExists(version: String?): Boolean {
        val path = version?.let { cachePath(it) }
        return path?.exists() ?: false
    }

    private fun cachePath(version: String): Path {
        return Path("local/metadata/cache/$name/$version/$name-$version" + language.suffix())
    }

    fun version(version: String): PlatformVersion? {
        return versions.firstOrNull { it.version == version }
    }

    fun tasks(): List<PlatformTask> {
        return tasks.map { PlatformTaskPool.find(it)!! }.toList()
    }

    fun preTasks(): List<PlatformTask> {
        return preTasks.map { PlatformTaskPool.find(it)!! }.toList()
    }

    private fun osDownloadName(): String {
        return osNameMapping.getOrElse(currentOS) { currentOS.name }
    }

    private fun archDownloadName(): String {
        return archNameMapping.getOrElse(currentCPUArchitecture) { currentCPUArchitecture }
    }
}
