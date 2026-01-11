package dev.httpmarco.polocloud.common.dependency.insert

import dev.httpmarco.polocloud.common.dependency.Dependency
import dev.httpmarco.polocloud.common.dependency.classlaoder.UrlClassLoader.url
import java.lang.reflect.Method
import java.net.URI
import java.net.URL
import java.net.URLClassLoader

/**
 * ClasspathInsert is responsible for dynamically adding dependencies
 * (as URLs) to the runtime classpath.
 *
 * This class converts a [Dependency] into a [URL] and inserts it
 * into the current ClassLoader if it is a [URLClassLoader].
 *
 * Note: This uses reflection to access the protected `addURL` method
 * of [URLClassLoader], which may not work in all environments (e.g., Java 9+ modular system).
 */
class ClasspathInsert : DependencyInsert<URL>() {

    /**
     * Converts a [Dependency] into a [URL] object.
     *
     * @param dependency the dependency containing the URL string
     * @return the URL representing the dependency
     */
    override fun renderDependency(dependency: Dependency): URL {
        return URI(dependency.url).toURL()
    }

    /**
     * Dynamically adds a [URL] to the current classloader's classpath.
     *
     * @param element the URL to add to the classpath
     */
    override fun connect(element: URL) {
        val classloader = this.javaClass.classLoader

        if (classloader is URLClassLoader) {
            classloader.url(element)
        } else {
            throw IllegalStateException("Current ClassLoader is not a URLClassLoader and cannot be modified")
        }
    }
}
