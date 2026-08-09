package de.polocloud.node.module

import java.io.File
import java.net.URLClassLoader

/**
 * One classloader per loaded module jar, child-first for everything except the shared SDK
 * surface: a module can bundle its own version of a library without clashing with the
 * node's own copy (or another module's), while [PolocloudModule][de.polocloud.moduleapi.PolocloudModule],
 * [de.polocloud.api.Polocloud] and the rest of module-api/api/common/shared/proto always
 * resolve to the single instance loaded by the node — otherwise casting a module instance
 * to `PolocloudModule` (or handing it a `ModuleNode`) across two different definitions of
 * that same class would fail with `ClassCastException`.
 */
class ModuleClassLoader(
    jarFile: File,
    parent: ClassLoader,
) : URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent) {

    /** Adds another jar (e.g. a module-declared Maven dependency) onto this module's own classpath. */
    fun addJar(file: File) {
        addURL(file.toURI().toURL())
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return it }

            if (SHARED_PREFIXES.any { name.startsWith(it) }) {
                return super.loadClass(name, resolve)
            }

            return try {
                findClass(name).also { if (resolve) resolveClass(it) }
            } catch (_: ClassNotFoundException) {
                super.loadClass(name, resolve)
            }
        }
    }

    private companion object {
        val SHARED_PREFIXES = listOf(
            "de.polocloud.moduleapi.",
            "de.polocloud.api.",
            "de.polocloud.shared.",
            "de.polocloud.proto.",
            "de.polocloud.common.",
            "kotlin.",
            "kotlinx.",
            "org.slf4j.",
            "java.",
            "javax.",
        )
    }
}
