package dev.httpmarco.polocloud.agent.module

import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.utils.Reloadable
import dev.httpmarco.polocloud.common.json.GSON
import dev.httpmarco.polocloud.common.version.VersionChecker
import dev.httpmarco.polocloud.common.version.polocloudVersion
import dev.httpmarco.polocloud.shared.module.LoadedModule
import dev.httpmarco.polocloud.shared.module.ModuleMetadata
import dev.httpmarco.polocloud.shared.module.PolocloudModule
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.notExists

/**
 * Manages the lifecycle of PoloCloud modules.
 *
 * This provider handles loading, unloading, and reloading of modules from JAR files.
 * Modules are loaded from the `local/modules` directory and must contain a valid
 * `module.json` metadata file.
 *
 * Features:
 * - Load order management (STARTUP, POST_STARTUP, LATE)
 * - API version validation
 */
class ModuleProvider : Reloadable {

    private val modulePath = Path("local/modules")
    private val loadedModules = mutableListOf<LoadedModule>()

    init {
        ensureModuleDirectoryExists()
    }

    /**
     * Reloads all modules by unloading existing modules and loading them again.
     */
    override fun reload() {
        unloadModules()
        loadModules()
    }

    /**
     * Loads all modules from module directory.
     *
     * Scans for JAR files, reads their metadata, validates API version,
     * sorts by load order, and initializes valid modules.
     * Successfully loaded modules will have their [PolocloudModule.onEnable] method called.
     * Any failures during loading are logged with details.
     */
    fun loadModules() {
        val discoveredModules = discoverAndValidateModules()
        val sortedModules = sortModulesByLoadOrder(discoveredModules)

        val (successful, failed) = sortedModules
            .map { (file, metadata) -> loadModule(file, metadata) }
            .partition { it.second }

        logLoadingResults(successful, failed)
    }

    /**
     * Unloads all currently loaded modules.
     *
     * Calls [PolocloudModule.onDisable] on each module in reverse load order
     * and closes their class loaders.
     * Any failures during unloading are logged as warnings.
     */
    fun unloadModules() {
        // Unload in reverse order (LATE -> POST_STARTUP -> STARTUP)
        loadedModules.asReversed().forEach { module ->
            runCatching {
                module.polocloudModule.onDisable()
                module.classLoader.close()
            }.onSuccess {
                i18n.info("agent.module.unload.successful", module.metadata.id)
            }.onFailure { exception ->
                i18n.warn("agent.module.unload.failed", module.metadata.id, exception)
            }
        }
        loadedModules.clear()
    }

    private fun ensureModuleDirectoryExists() {
        if (modulePath.notExists()) {
            Files.createDirectories(this.modulePath)
        }
    }

    private fun discoverModules(): List<File> {
        return this.modulePath.toFile()
            .listFiles { _, name -> name.endsWith(".jar") }
            ?.toList()
            .orEmpty()
    }

    private fun discoverAndValidateModules(): Map<File, ModuleMetadata> {
        return discoverModules()
            .mapNotNull { file ->
                val metadata = readModuleMetadata(file) ?: return@mapNotNull null

                if (!isApiVersionCompatible(metadata)) {
                    i18n.error("agent.module.incompatible.version", metadata.id, metadata.apiVersion, polocloudVersion())
                    return@mapNotNull null
                }

                file to metadata
            }
            .toMap()
    }

    private fun sortModulesByLoadOrder(modules: Map<File, ModuleMetadata>): List<Pair<File, ModuleMetadata>> {
        return modules.entries
            .sortedBy { it.value.loadOrder }
            .map { it.key to it.value }
    }

    private fun loadModule(file: File, metadata: ModuleMetadata): Pair<String, Boolean> {
        return runCatching {
            i18n.info("agent.module.loading", metadata.name, metadata.loadOrder.name)
            createLoadedModule(file, metadata).also { loadedModule ->
                loadedModule.polocloudModule.onEnable()
                i18n.info("agent.module.enabled", metadata.name)
            }
        }.fold(
            onSuccess = { metadata.name to true },
            onFailure = { exception ->
                i18n.error("agent.module.load.failed", metadata.id)
                exception.printStackTrace()
                metadata.name to false
            }
        )
    }

    private fun isApiVersionCompatible(metadata: ModuleMetadata): Boolean {
        val currentVersion = polocloudVersion()
        return VersionChecker.isCompatible(metadata.apiVersion, currentVersion)
    }

    private fun createLoadedModule(file: File, metadata: ModuleMetadata): LoadedModule {
        val classLoader = URLClassLoader(
            arrayOf(file.toURI().toURL()),
            this::class.java.classLoader
        )

        val mainClass = classLoader.loadClass(metadata.main)

        require(PolocloudModule::class.java.isAssignableFrom(mainClass)) {
            i18n.error("agent.module.implementation.missing", metadata.id)
        }

        val moduleInstance = mainClass
            .getDeclaredConstructor()
            .newInstance() as PolocloudModule

        return LoadedModule(moduleInstance, classLoader, metadata).also { loadedModule ->
            loadedModules += loadedModule
        }
    }

    private fun readModuleMetadata(file: File): ModuleMetadata? {
        return runCatching {
            JarFile(file).use { jar ->
                val metadataEntry = jar.getJarEntry("module.json") ?: return null
                jar.getInputStream(metadataEntry).use { stream ->
                    GSON.fromJson(stream.reader(), ModuleMetadata::class.java)
                }
            }
        }.onFailure { _ ->
            i18n.error("agent.module.metadata.read.failed", file.name)
        }.getOrNull()
    }

    private fun logLoadingResults(successful: List<Pair<String, Boolean>>, failed: List<Pair<String, Boolean>>) {
        val statusMessage = buildList {
            addAll(successful.map { "&3${it.first}" })
            addAll(failed.map { "&c${it.first}" })
        }

        if (statusMessage.isNotEmpty()) {
            i18n.info( "agent.module.load.successful", statusMessage.joinToString("&8, "))
        }
    }

}