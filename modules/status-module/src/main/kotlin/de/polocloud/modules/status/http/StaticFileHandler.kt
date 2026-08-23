package de.polocloud.modules.status.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.net.URLConnection

/**
 * Serves the status website bundled into this module's own jar under
 * `src/main/resources/webroot/` — no separate file layout needed on the node, the site
 * ships as part of the module. `index.html` is the fallback for `/` and for any path
 * that doesn't resolve to a bundled file (client-side routing / plain `/status`-style URLs).
 */
class StaticFileHandler : HttpHandler {

    override fun handle(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") {
            exchange.respondBytes(405, "method not allowed".toByteArray(), "text/plain; charset=utf-8")
            return
        }

        val resourcePath = "webroot/" + normalize(exchange.requestURI.path)
        val resource = javaClass.classLoader.getResource(resourcePath)
            ?: javaClass.classLoader.getResource("webroot/index.html")

        if (resource == null) {
            val message = "status website assets not found in this module's jar".toByteArray()
            exchange.respondBytes(404, message, "text/plain; charset=utf-8")
            return
        }

        val bytes = resource.openStream().use { it.readBytes() }
        exchange.respondBytes(200, if (exchange.requestMethod == "HEAD") ByteArray(0) else bytes, contentTypeFor(resourcePath))
    }

    /** Strips the leading `/` and collapses `..`/`.` segments so a request can't escape `webroot/`. */
    private fun normalize(path: String): String {
        val stack = ArrayDeque<String>()
        for (segment in path.split("/")) {
            when {
                segment.isEmpty() || segment == "." -> {}
                segment == ".." -> stack.removeLastOrNull()
                else -> stack.addLast(segment)
            }
        }
        return stack.joinToString("/").ifBlank { "index.html" }
    }

    private fun contentTypeFor(path: String): String = when {
        path.endsWith(".html") -> "text/html; charset=utf-8"
        path.endsWith(".css") -> "text/css; charset=utf-8"
        path.endsWith(".js") -> "application/javascript; charset=utf-8"
        path.endsWith(".json") -> "application/json; charset=utf-8"
        path.endsWith(".svg") -> "image/svg+xml"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".ico") -> "image/x-icon"
        else -> URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
    }
}
