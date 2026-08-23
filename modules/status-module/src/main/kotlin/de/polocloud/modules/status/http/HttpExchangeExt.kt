package de.polocloud.modules.status.http

import com.sun.net.httpserver.HttpExchange
import java.nio.charset.StandardCharsets

fun HttpExchange.respondBytes(status: Int, bytes: ByteArray, contentType: String) {
    responseHeaders.add("Content-Type", contentType)
    sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
    if (bytes.isNotEmpty()) {
        responseBody.use { it.write(bytes) }
    } else {
        responseBody.close()
    }
}

fun HttpExchange.respondJson(status: Int, body: String) =
    respondBytes(status, body.toByteArray(StandardCharsets.UTF_8), "application/json; charset=utf-8")
