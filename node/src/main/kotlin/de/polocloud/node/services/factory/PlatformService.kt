package de.polocloud.node.services.factory

import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.custom.CustomPlatformRepository
import de.polocloud.node.services.factory.platform.custom.toPlatform
import de.polocloud.node.services.factory.task.TaskDefinition
import de.polocloud.node.services.factory.task.loadTaskDefinitionsFromCache
import de.polocloud.node.services.factory.template.PlatformDownloader
import de.polocloud.node.services.factory.template.convertTemplatesToPlatform
import de.polocloud.node.services.factory.template.loadTemplatesFromCache
import org.slf4j.LoggerFactory
import java.io.File

class PlatformService {

    private val logger = LoggerFactory.getLogger(PlatformService::class.java)
    private val platforms = mutableMapOf<String, Platform>()
    private val taskDefinitions = mutableMapOf<String, TaskDefinition>()
    private val cacheDir = File(".cache/platforms")

    fun load() {
        var templates = loadTemplatesFromCache(cacheDir)
        if (templates.isEmpty()) {
            logger.info("No platform templates found in {} — fetching latest release ...", cacheDir.path)
            runCatching { PlatformDownloader.downloadInto(cacheDir) }
                .onFailure { logger.error("Failed to download platform templates", it) }
        }
        templates = loadTemplatesFromCache(cacheDir)
        if (templates.isEmpty()) {
            logger.warn("No platform templates available in {}", cacheDir.path)
        } else {
            val resolved = convertTemplatesToPlatform(templates)
            resolved.forEach { platforms[it.name] = it }
        }

        taskDefinitions.putAll(loadTaskDefinitionsFromCache(cacheDir))

        loadCustomPlatforms()

        logger.info("Loaded {} platform(s): {}", platforms.size, platforms.keys.joinToString())
        if (taskDefinitions.isNotEmpty()) {
            logger.info("Loaded {} task definition(s): {}", taskDefinitions.size, taskDefinitions.keys.joinToString())
        }
    }

    /**
     * Force re-downloads the latest `polocloud-platforms` template bundle and rebuilds the
     * built-in platforms/task definitions from it, regardless of whether [cacheDir] already
     * has content — unlike [load], which only downloads when the cache is empty. Custom
     * platforms are left untouched. Used by the `reload` command to resync platform versions
     * without requiring a full node restart.
     */
    fun resync() {
        val download = runCatching { PlatformDownloader.downloadInto(cacheDir) }
        if (download.isFailure) {
            logger.error("Failed to download platform templates", download.exceptionOrNull())
            return
        }

        val templates = loadTemplatesFromCache(cacheDir)
        if (templates.isEmpty()) {
            logger.warn("No platform templates available in {}", cacheDir.path)
            return
        }

        platforms.entries.removeAll { !it.value.custom }
        convertTemplatesToPlatform(templates).forEach { platforms[it.name] = it }

        taskDefinitions.clear()
        taskDefinitions.putAll(loadTaskDefinitionsFromCache(cacheDir))

        logger.info("Resynced {} platform(s): {}", platforms.size, platforms.keys.joinToString())
        if (taskDefinitions.isNotEmpty()) {
            logger.info("Loaded {} task definition(s): {}", taskDefinitions.size, taskDefinitions.keys.joinToString())
        }
    }

    /**
     * Loads every persisted [de.polocloud.node.services.factory.platform.custom.CustomPlatform]
     * and merges it into [platforms]. Unlike built-in platforms, these survive a node restart
     * by being read back here rather than re-derived from the downloaded template bundle.
     *
     * A name collision with an already-loaded built-in platform is skipped with a warning —
     * [de.polocloud.node.services.factory.platform.custom.CustomPlatformService.create] is the
     * primary guard against this happening in the first place; this is only a safety net (e.g.
     * a built-in platform introduced later that happens to reuse an existing custom name).
     */
    private fun loadCustomPlatforms() {
        CustomPlatformRepository.findAll().forEach { custom ->
            val existing = platforms[custom.name]
            if (existing != null && !existing.custom) {
                logger.warn(
                    "Custom platform '{}' collides with a built-in platform of the same name — ignoring the custom one",
                    custom.name
                )
                return@forEach
            }
            platforms[custom.name] = custom.toPlatform()
        }
    }

    fun find(name: String): Platform? = platforms[name]

    fun all(): Collection<Platform> = platforms.values

    /** Platforms created by an operator via `platform setup`, as opposed to [builtInPlatforms]. */
    fun customPlatforms(): Collection<Platform> = platforms.values.filter { it.custom }

    /** Platforms shipped in the default `polocloud-platforms` template bundle. */
    fun builtInPlatforms(): Collection<Platform> = platforms.values.filterNot { it.custom }

    /**
     * Adds or replaces [platform] in the live registry. Used by
     * [de.polocloud.node.services.factory.platform.custom.CustomPlatformService] so a
     * newly created/updated custom platform is immediately usable (e.g. by `group create`)
     * without waiting for a node restart to re-run [load].
     */
    fun registerCustom(platform: Platform) {
        require(platform.custom) { "registerCustom() is only for custom platforms, got '${platform.name}'" }
        platforms[platform.name] = platform
    }

    /** Removes a custom platform from the live registry, e.g. after it is deleted. */
    fun unregister(name: String) {
        platforms.remove(name)
    }

    /** The platform's own cache directory (e.g. `.cache/platforms/velocity`), used as
     *  the source for [de.polocloud.node.services.factory.task.TaskStepType.COPY_FILE_IF_NOT_EXISTS] steps. */
    fun directoryFor(platform: Platform): File = File(cacheDir, platform.name)

    /** All loaded task definitions, keyed by [TaskDefinition.key]. */
    fun taskDefinitions(): Map<String, TaskDefinition> = taskDefinitions
}
