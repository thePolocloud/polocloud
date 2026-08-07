package de.polocloud.node.communication.handler.services

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.StopServiceRequest
import de.polocloud.shared.service.ServiceState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Covers [CallerAuthorization][de.polocloud.node.communication.handler.CallerAuthorization]'s
 * wiring into [StopServiceServerHandler]: a launched service (a "serviceSubject" in the
 * context — see [de.polocloud.node.communication.interceptor.ServiceIdentityInterceptor])
 * may only stop itself, never another service in the cluster.
 */
class StopServiceServerHandlerTest {

    private fun provider(vararg names: String): ServiceProvider {
        val provider = ServiceProvider()
        names.forEach { name ->
            val (group, index) = name.substringBeforeLast('-') to name.substringAfterLast('-').toInt()
            provider.localServices += LocalService(
                Service(UUID.randomUUID(), index, group, ServiceState.RUNNING, "127.0.0.1", 30000, "")
            )
        }
        return provider
    }

    private fun serviceContext(id: String) = GrpcServerContext().with("serviceSubject", id)

    @Test
    fun `a service can stop itself`() = runBlocking {
        val handler = StopServiceServerHandler(provider("lobby-1"))

        val response = handler.handle(
            StopServiceRequest.newBuilder().setServiceName("lobby-1").build(),
            serviceContext("lobby-1"),
        )

        assertTrue(response.stopped)
    }

    @Test
    fun `a service cannot stop another service`() {
        val handler = StopServiceServerHandler(provider("lobby-1", "proxy-1"))

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    StopServiceRequest.newBuilder().setServiceName("proxy-1").build(),
                    serviceContext("lobby-1"),
                )
            }
        }
    }

    @Test
    fun `a trusted CLI or peer caller can stop any service`() = runBlocking {
        val handler = StopServiceServerHandler(provider("lobby-1", "proxy-1"))

        val response = handler.handle(
            StopServiceRequest.newBuilder().setServiceName("proxy-1").build(),
            GrpcServerContext(),
        )

        assertTrue(response.stopped)
    }
}
