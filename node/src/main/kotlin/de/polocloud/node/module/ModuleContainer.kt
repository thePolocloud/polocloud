package de.polocloud.node.module

import de.polocloud.moduleapi.ModuleDescriptor
import de.polocloud.moduleapi.PolocloudModule
import java.io.File

/** A single loaded module: its parsed descriptor, live instance, source jar and dedicated classloader. */
class ModuleContainer(
    val descriptor: ModuleDescriptor,
    val instance: PolocloudModule,
    val classLoader: ModuleClassLoader,
    val jarFile: File,
) {
    var enabled: Boolean = false
        internal set
}
