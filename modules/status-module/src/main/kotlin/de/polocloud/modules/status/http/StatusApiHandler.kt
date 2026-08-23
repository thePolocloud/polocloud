package de.polocloud.modules.status.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import de.polocloud.modules.status.StatusConfig
import de.polocloud.modules.status.StatusService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger

/**
 * REST endpoints, mounted at `/api/status`:
 * - `GET /api/status` — the full [de.polocloud.modules.status.StatusSnapshot].
 * - `GET /api/status/groups/{name}` — a single group, `404` if it isn't published (see
 *   [StatusConfig.groups]).
 */
class StatusApiHandler(
    private val config: StatusConfig,
    private val statusService: StatusService,
    private val logger: Logger,
) : HttpHandler {

    private val json = Json { encodeDefaults = true }

    override fun handle(exchange: HttpExchange) {
        try {
            if (config.corsEnabled) {
                exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            }

            if (exchange.requestMethod == "OPTIONS") {
                exchange.respondBytes(204, ByteArray(0), "text/plain")
                return
            }
            if (exchange.requestMethod != "GET") {
                exchange.respondJson(405, """{"error":"method not allowed"}""")
                return
            }

            val subPath = exchange.requestURI.path.removePrefix("/api/status").trim('/')
            when {
                subPath.isEmpty() ->
                    exchange.respondJson(200, json.encodeToString(statusService.snapshot()))

                subPath.startsWith("groups/") -> {
                    val name = subPath.removePrefix("groups/").trim('/')
                    val group = statusService.group(name)
                    if (group == null) {
                        exchange.respondJson(404, """{"error":"unknown group '$name'"}""")
                    } else {
                        exchange.respondJson(200, json.encodeToString(group))
                    }
                }

                else -> exchange.respondJson(404, """{"error":"not found"}""")
            }
        } catch (e: Exception) {
            logger.error("Failed to handle {}: {}", exchange.requestURI, e.message, e)
            exchange.respondJson(500, """{"error":"internal error"}""")
        }
    }
}
