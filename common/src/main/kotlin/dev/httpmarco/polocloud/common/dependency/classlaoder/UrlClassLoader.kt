package dev.httpmarco.polocloud.common.dependency.classlaoder

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

object UrlClassLoader {

    /**
     * Adds a URL to the given [URLClassLoader] using reflection.
     *
     * @param url the URL to add
     */
    fun URLClassLoader.url(url: URL) {
        val method: Method = Thread.currentThread().contextClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
        method.isAccessible = true
        method.invoke(this, url)
    }
}