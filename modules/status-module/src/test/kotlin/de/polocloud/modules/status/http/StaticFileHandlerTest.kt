package de.polocloud.modules.status.http

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class StaticFileHandlerTest {

    private lateinit var server: HttpServer
    private val client = HttpClient.newHttpClient()

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", StaticFileHandler())
        server.start()
    }

    @AfterEach
    fun stop() = server.stop(0)

    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.address.port}$path")).GET().build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `root serves the bundled index html`() {
        val response = get("/")
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("PoloCloud Status"))
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"))
    }

    @Test
    fun `bundled assets are served with the right content type`() {
        val css = get("/style.css")
        assertEquals(200, css.statusCode())
        assertEquals("text/css; charset=utf-8", css.headers().firstValue("Content-Type").orElse(""))

        val js = get("/app.js")
        assertEquals(200, js.statusCode())
        assertEquals("application/javascript; charset=utf-8", js.headers().firstValue("Content-Type").orElse(""))
    }

    @Test
    fun `an unknown path falls back to index html instead of a bare 404`() {
        val response = get("/does/not/exist")
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("PoloCloud Status"))
    }

    @Test
    fun `path traversal cannot escape the bundled webroot`() {
        val response = get("/../../../../etc/passwd")
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("PoloCloud Status"))
    }
}
