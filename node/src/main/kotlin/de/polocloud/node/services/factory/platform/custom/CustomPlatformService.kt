package de.polocloud.node.services.factory.platform.custom

import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.PlatformVersionSource
import de.polocloud.node.services.factory.process.PlatformJarCache
import java.io.File

/**
 * Manages operator-defined custom platforms: creation, attaching further versions, and
 * deletion. Sits above [CustomPlatformRepository] the same way
 * [de.polocloud.node.group.GroupService] sits above `GroupRepository` — the terminal layer
 * (`PlatformCommand`, `PlatformSetupWizard`, `PlatformVersionSetupWizard`) only ever calls in
 * here, never the repository directly.
 *
 * Every mutating call also pushes the resulting [de.polocloud.node.services.factory.platform.Platform]
 * into [platformService] so a newly created/updated custom platform is immediately usable
 * (e.g. by `group create`) without a node restart.
 */
class CustomPlatformService(private val platformService: PlatformService) {

    fun exists(name: String) = CustomPlatformRepository.exists(name)

    fun find(name: String) = CustomPlatformRepository.find(name)

    fun findAll() = CustomPlatformRepository.findAll()

    /**
     * Creates a new custom platform with no versions yet attached. Callers must first confirm
     * [name] doesn't collide with any loaded platform — built-in or custom — via
     * `platformService.find(name)`; this only guards the custom-platform table itself.
     */
    fun create(name: String, type: String, language: String): CustomPlatform {
        val platform = CustomPlatform(name = name, type = type.uppercase(), language = language.uppercase(), versionsJson = "[]")
        CustomPlatformRepository.save(platform)
        platformService.registerCustom(platform.toPlatform())
        return platform
    }

    /**
     * Attaches a new version to [platform], sourced either from a URL or a jar already present
     * on this node's filesystem.
     *
     * [location] is validated with [PlatformSourceValidator] first — an unreachable URL or a
     * missing/corrupt local file is rejected here, before anything is persisted. A validated
     * [PlatformVersionSource.LOCAL_FILE] is immediately copied into the shared
     * [PlatformJarCache], so the platform's availability no longer depends on the original path
     * still existing by the time a service actually starts.
     *
     * @throws IllegalArgumentException if [version] is already attached, or the source fails validation.
     */
    fun addVersion(platform: CustomPlatform, version: String, source: PlatformVersionSource, location: String): CustomPlatform {
        require(platform.versions.none { it.version == version }) {
            "Platform '${platform.name}' already has a version '$version'."
        }

        val rejectionReason = when (source) {
            PlatformVersionSource.URL -> PlatformSourceValidator.verifyUrl(location)
            PlatformVersionSource.LOCAL_FILE -> PlatformSourceValidator.verifyLocalFile(location, requireJar = platform.language == "JAVA")
        }
        require(rejectionReason == null) { rejectionReason!! }

        if (source == PlatformVersionSource.LOCAL_FILE) {
            PlatformJarCache.DIRECTORY.mkdirs()
            val cacheFile = PlatformJarCache.fileFor(platform.name, version, CUSTOM_PLATFORM_BUILD)
            File(location).copyTo(cacheFile, overwrite = true)
        }

        val updated = platform.copy(
            versionsJson = CustomPlatformVersionCodec.encode(
                platform.versions + CustomPlatformVersion(version, source, location)
            )
        )
        CustomPlatformRepository.save(updated)
        platformService.registerCustom(updated.toPlatform())
        return updated
    }

    fun delete(platform: CustomPlatform) {
        CustomPlatformRepository.delete(platform)
        platformService.unregister(platform.name)
    }
}
