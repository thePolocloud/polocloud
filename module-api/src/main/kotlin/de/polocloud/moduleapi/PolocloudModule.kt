package de.polocloud.moduleapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Base class for a polocloud node module. Modules are plain jars dropped into
 * `local/modules/` on a node, described by a `module.yml` at their jar root (see
 * [ModuleDescriptor]), and are (re)loaded whenever the node starts or `reload` runs.
 *
 * A subclass needs a public no-arg constructor — the loader instantiates it via
 * reflection before [descriptor]/[node] exist, then calls [attach] once to wire them up.
 *
 * Lifecycle, in order:
 * - [onLoad] — called once the module is instantiated and attached, before any module is
 *   enabled. Register listeners/state that other modules' [onEnable] might depend on here.
 * - [onEnable] — called once this module (and, for [ModuleScope.SINGLE_ACTIVE] modules,
 *   this node) is actually active. Start doing work here.
 * - [onDisable] — called on node shutdown, on a `reload`, or when a
 *   [ModuleScope.SINGLE_ACTIVE] module's node loses cluster head status. Release
 *   whatever [onEnable] acquired.
 */
abstract class PolocloudModule {

    lateinit var descriptor: ModuleDescriptor
        private set

    lateinit var node: ModuleNode
        private set

    private var attached = false

    val logger: Logger by lazy { LoggerFactory.getLogger("module/${descriptor.name}") }

    val dataFolder: File get() = node.dataFolder

    /** Called once by the loader right after construction — not meant to be called by module code. */
    fun attach(descriptor: ModuleDescriptor, node: ModuleNode) {
        check(!attached) { "Module '${descriptor.name}' is already attached" }
        this.descriptor = descriptor
        this.node = node
        attached = true
    }

    open fun onLoad() {}

    open fun onEnable() {}

    open fun onDisable() {}
}
