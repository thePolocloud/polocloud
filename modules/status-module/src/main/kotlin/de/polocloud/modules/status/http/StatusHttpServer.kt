package de.polocloud.modules.status.http

import com.sun.net.httpserver.HttpServer
import de.polocloud.modules.status.StatusConfig
import de.polocloud.modules.status.StatusService
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * The status module's own tiny HTTP server — deliberately built on the JDK's built-in
 * [HttpServer] instead of a full framework: modules resolve extra [StatusConfig]
 * dependencies from Maven Central individually with no transitive resolution (see
 * [de.polocloud.moduleapi.ModuleDescriptor.dependencies]), so anything heavier would mean
 * hand-listing that framework's whole dependency tree. This needs none of that.
 */
class StatusHttpServer(
    private val config: StatusConfig,
    private val statusService: StatusService,
    private val logger: Logger,
) : AutoCloseable {

    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val server = HttpServer.create(InetSocketAddress(config.host, config.port), 0)

    fun start() {
        server.executor = executor
        server.createContext("/api/status", StatusApiHandler(config, statusService, logger))
        if (config.hostWebsite) {
            server.createContext("/", StaticFileHandler())
        }
        server.start()
        logger.info(
            "Status API listening on http://{}:{}/api/status{}",
            config.host, config.port,
            if (config.hostWebsite) " (website at http://${config.host}:${config.port}/)" else "",
        )
    }

    override fun close() {
        server.stop(0)
        executor.shutdown()
    }
}
