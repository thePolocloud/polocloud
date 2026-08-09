package de.polocloud.node.module

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.common.version.PolocloudVersionParser
import de.polocloud.moduleapi.ModuleDescriptor
import de.polocloud.moduleapi.ModuleScope
import de.polocloud.moduleapi.PolocloudModule
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.utils.isSafePathSegment
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Loads every module jar under `local/modules/` on node start and on `reload` (and, once
 * running, whenever a jar is added/changed/removed there — see [ModuleFolderWatcher]),
 * and keeps [ModuleScope.SINGLE_ACTIVE] modules enabled on exactly one node cluster-wide
 * by reacting to head changes (see [NodeElectionService.addLeadershipListener]).
 *
 * Load order for a single [loadAll]/[reload] pass:
 *  1. every jar's `module.yml` is read
 *  2. modules are topologically sorted by `depends`/`soft-depends`
 *  3. each module is instantiated, attached and [PolocloudModule.onLoad]-ed, in that order
 *  4. each successfully loaded module is then enabled, in that order — unless it's
 *     [ModuleScope.SINGLE_ACTIVE] and this node isn't currently head, in which case it
 *     stays loaded but on standby until this node becomes head (or forever, if it doesn't).
 *
 * Every [PolocloudModule.onLoad]/[PolocloudModule.onEnable]/[PolocloudModule.onDisable]
 * call runs behind [LIFECYCLE_TIMEOUT] (see [runWithTimeout]) so one module blocking
 * forever (e.g. an un-timed-out network call) can't hang node startup or a `reload`.
 */
class ModuleManager(
    private val context: NodeRuntimeContext,
    private val electionService: NodeElectionService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val folder = File("local/modules")
    private val dataFolder = File(folder, "data")

    // Insertion order doubles as load order, which unloadAll()/unload() reverse for a
    // correct teardown (dependents disabled before their dependencies).
    private val modules = LinkedHashMap<String, ModuleContainer>()

    // Keyed by module name once a descriptor was readable, otherwise by jar file name —
    // so a jar that fails to even parse still shows up somewhere. Cleared per-key on the
    // next successful load of that same key.
    private val failures = LinkedHashMap<String, String>()

    private var watcher: ModuleFolderWatcher? = null

    // Bounded, so onLoad/onEnable/onDisable can't hang the calling thread (node startup,
    // the `reload` command, the folder watcher, ...) forever. Daemon threads: a module
    // that ignores its interrupt and keeps running past the timeout must never keep the
    // JVM alive on its own.
    private val lifecycleExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "polocloud-module-lifecycle").apply { isDaemon = true }
    }

    init {
        folder.mkdirs()
        electionService.addLeadershipListener(::onLeadershipChanged)
    }

    fun list(): List<ModuleContainer> = modules.values.toList()

    fun find(name: String): ModuleContainer? = modules[name]

    /** Reasons the most recent [loadAll]/[reload]/watcher pass failed to load a module, keyed by module (or jar) name. */
    fun failures(): Map<String, String> = failures.toMap()

    /** Scans [folder] for jars, loads/enables every module found, then starts watching it for changes. */
    fun loadAll() {
        val jars = folder.listFiles { file -> file.isFile && file.extension == "jar" }
            ?.sortedBy { it.name }
            ?: emptyList()

        failures.clear()

        if (jars.isNotEmpty()) {
            val descriptorsByJar = LinkedHashMap<ModuleDescriptor, File>()
            for (jar in jars) {
                val descriptor = try {
                    ModuleDescriptorParser.parse(jar)
                } catch (e: Exception) {
                    fail(jar.name, e.message ?: e.toString())
                    continue
                }
                val existing = descriptorsByJar.keys.find { it.name == descriptor.name }
                if (existing != null) {
                    fail(jar.name, "duplicate module name '${descriptor.name}' (also in '${descriptorsByJar.getValue(existing).name}')")
                    continue
                }
                descriptorsByJar[descriptor] = jar
            }

            val order = resolveLoadOrder(descriptorsByJar.keys)
            for (descriptor in order) {
                load(descriptorsByJar.getValue(descriptor), descriptor)
            }

            val headNow = electionService.isHead()
            for (descriptor in order) {
                val container = modules[descriptor.name] ?: continue
                if (shouldBeEnabled(container.descriptor.scope, headNow)) {
                    enable(container)
                } else {
                    logger.info("Module '{}' is SINGLE_ACTIVE and this node isn't the cluster head — staying on standby", descriptor.name)
                }
            }
        }

        startWatching()
    }

    /** Disables and unloads every currently loaded module, closing its classloader, and stops watching [folder]. */
    fun unloadAll() {
        stopWatching()
        for (container in modules.values.toList().asReversed()) {
            unload(container)
        }
    }

    /** Unloads and re-loads every module from [folder] from scratch. */
    fun reload() {
        unloadAll()
        loadAll()
    }

    /** Unloads and re-loads a single module by name. Returns `false` if it isn't currently loaded. */
    fun reload(name: String): Boolean {
        val container = modules[name] ?: return false
        unload(container)

        val descriptor = try {
            ModuleDescriptorParser.parse(container.jarFile)
        } catch (e: Exception) {
            fail(name, e.message ?: e.toString())
            return true
        }
        load(container.jarFile, descriptor)
        modules[descriptor.name]?.let { reloaded ->
            if (shouldBeEnabled(reloaded.descriptor.scope, electionService.isHead())) enable(reloaded)
        }
        return true
    }

    /** Enables an already-loaded, currently disabled/standby module in place. Returns `false` if it isn't loaded. */
    fun enable(name: String): Boolean {
        val container = modules[name] ?: return false
        enable(container)
        return true
    }

    /** Disables an already-loaded module in place, without unloading it. Returns `false` if it isn't loaded. */
    fun disable(name: String): Boolean {
        val container = modules[name] ?: return false
        disable(container)
        return true
    }

    private fun unload(container: ModuleContainer) {
        disable(container)
        runCatching { container.classLoader.close() }
            .onFailure { logger.warn("Failed to close classloader for module '{}': {}", container.descriptor.name, it.message) }
        modules.remove(container.descriptor.name)
    }

    private fun load(jar: File, descriptor: ModuleDescriptor) {
        for (dependency in descriptor.depends) {
            if (dependency !in modules) {
                fail(descriptor.name, "depends on '$dependency', which isn't loaded")
                return
            }
        }

        descriptor.apiVersion?.let { declaredRaw ->
            val declared = runCatching { PolocloudVersionParser.parse(declaredRaw) }.getOrNull()
            if (declared == null) {
                fail(descriptor.name, "declares an invalid api-version '$declaredRaw'")
                return
            }
            if (!isApiCompatible(PolocloudVersion.CURRENT, declared)) {
                fail(
                    descriptor.name,
                    "requires polocloud >= ${declared.toVersionString()} (major ${declared.major} must match) but this node runs ${PolocloudVersion.CURRENT.toVersionString()}"
                )
                return
            }
        }

        var classLoader: ModuleClassLoader? = null
        try {
            classLoader = ModuleClassLoader(jar, javaClass.classLoader)

            for (coordinate in descriptor.dependencies) {
                val dependencyJar = ModuleDependencyResolver.resolve(coordinate)
                classLoader.addJar(dependencyJar)
            }

            val moduleClass = Class.forName(descriptor.main, true, classLoader)
            require(PolocloudModule::class.java.isAssignableFrom(moduleClass)) {
                "main class '${descriptor.main}' does not extend PolocloudModule"
            }
            val instance = moduleClass.getDeclaredConstructor().newInstance() as PolocloudModule

            require(isSafePathSegment(descriptor.name)) { "module name '${descriptor.name}' is not a valid folder name" }
            val moduleNode = ModuleNodeImpl(context, electionService, File(dataFolder, descriptor.name))

            instance.attach(descriptor, moduleNode)

            val loadError = runWithTimeout { instance.onLoad() }
            if (loadError != null) {
                fail(descriptor.name, "onLoad failed: $loadError")
                runCatching { classLoader.close() }
                return
            }

            val container = ModuleContainer(descriptor, instance, classLoader, jar)
            modules[descriptor.name] = container
            failures.remove(descriptor.name)
            logger.info("Loaded module '{}' v{}", descriptor.name, descriptor.version)
            publishStatus(container)
        } catch (e: Exception) {
            fail(descriptor.name, e.message ?: e.toString())
            runCatching { classLoader?.close() }
        }
    }

    private fun enable(container: ModuleContainer) {
        if (container.enabled) return
        val error = runWithTimeout { container.instance.onEnable() }
        if (error == null) {
            container.enabled = true
            failures.remove(container.descriptor.name)
            logger.info("Enabled module '{}'", container.descriptor.name)
        } else {
            fail(container.descriptor.name, "onEnable failed: $error")
        }
        publishStatus(container)
    }

    private fun disable(container: ModuleContainer) {
        if (!container.enabled) return
        val error = runWithTimeout { container.instance.onDisable() }
        container.enabled = false
        if (error == null) {
            logger.info("Disabled module '{}'", container.descriptor.name)
        } else {
            logger.error("Module '{}' onDisable failed: {}", container.descriptor.name, error)
        }
        publishStatus(container)
    }

    /**
     * Runs [block] on [lifecycleExecutor] and waits up to [LIFECYCLE_TIMEOUT] for it.
     * Returns `null` on success, otherwise a human-readable error — either the exception
     * [block] threw, or a note that it timed out. A timeout only stops *this manager* from
     * waiting any longer: if [block] isn't cooperating with interruption (e.g. blocked on
     * non-interruptible I/O), its thread may keep running in the background regardless.
     */
    private fun runWithTimeout(timeout: Duration = LIFECYCLE_TIMEOUT, block: () -> Unit): String? {
        val future = lifecycleExecutor.submit(block)
        return try {
            future.get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            null
        } catch (e: TimeoutException) {
            future.cancel(true)
            "timed out after $timeout — it may still be running in the background"
        } catch (e: ExecutionException) {
            e.cause?.message ?: e.cause?.toString() ?: e.message ?: "unknown error"
        }
    }

    private fun fail(key: String, reason: String) {
        failures[key] = reason
        logger.error("Module '{}': {}", key, reason)
    }

    private fun publishStatus(container: ModuleContainer) {
        ClusterModuleRegistry.publish(
            container,
            context.localNodeContainer.data.id.toString(),
            context.localNodeContainer.data.name(),
        )
    }

    /** Flips every loaded [ModuleScope.SINGLE_ACTIVE] module's enabled state to match [isHead]. */
    private fun onLeadershipChanged(isHead: Boolean) {
        modules.values
            .filter { it.descriptor.scope == ModuleScope.SINGLE_ACTIVE }
            .forEach { container -> if (isHead) enable(container) else disable(container) }
    }

    private fun startWatching() {
        if (watcher != null) return
        watcher = ModuleFolderWatcher(folder, onJarChanged = ::handleJarChanged, onJarRemoved = ::handleJarRemoved)
    }

    private fun stopWatching() {
        watcher?.stop()
        watcher = null
    }

    private fun handleJarChanged(jar: File) {
        val descriptor = try {
            ModuleDescriptorParser.parse(jar)
        } catch (e: Exception) {
            fail(jar.name, e.message ?: e.toString())
            return
        }

        val existing = modules[descriptor.name]
        if (existing != null) {
            logger.info("Detected change to module '{}' — reloading", descriptor.name)
            unload(existing)
        } else {
            logger.info("Detected new module jar '{}' — loading", jar.name)
        }

        load(jar, descriptor)
        modules[descriptor.name]?.let { container ->
            if (shouldBeEnabled(container.descriptor.scope, electionService.isHead())) enable(container)
        }
    }

    private fun handleJarRemoved(jar: File) {
        val container = modules.values.find { it.jarFile == jar } ?: return
        logger.info("Detected removal of module jar '{}' — unloading '{}'", jar.name, container.descriptor.name)
        unload(container)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ModuleManager::class.java)

        private val LIFECYCLE_TIMEOUT = 15.seconds

        /** Same major version required; node's minor/patch must be at least [declared]'s. */
        internal fun isApiCompatible(current: PolocloudVersion, declared: PolocloudVersion): Boolean =
            current.major == declared.major &&
                (current.minor > declared.minor || (current.minor == declared.minor && current.patch >= declared.patch))

        internal fun shouldBeEnabled(scope: ModuleScope, isHead: Boolean): Boolean =
            scope == ModuleScope.EVERY_NODE || isHead

        /**
         * Kahn-style topological sort over `depends` + `soft-depends`. Missing soft
         * dependencies are silently ignored (they only influence ordering when present); a
         * cycle breaks the chain at the point it's detected and is logged, rather than
         * refusing to load anything.
         */
        internal fun resolveLoadOrder(descriptors: Collection<ModuleDescriptor>): List<ModuleDescriptor> {
            val byName = descriptors.associateBy { it.name }
            val visited = mutableSetOf<String>()
            val visiting = mutableSetOf<String>()
            val result = mutableListOf<ModuleDescriptor>()

            fun visit(descriptor: ModuleDescriptor) {
                if (descriptor.name in visited) return
                if (descriptor.name in visiting) {
                    logger.error("Circular module dependency detected involving '{}' — breaking the cycle here", descriptor.name)
                    return
                }
                visiting += descriptor.name
                for (dependencyName in descriptor.depends + descriptor.softDepends) {
                    byName[dependencyName]?.let(::visit)
                }
                visiting -= descriptor.name
                visited += descriptor.name
                result += descriptor
            }

            descriptors.forEach(::visit)
            return result
        }
    }
}
