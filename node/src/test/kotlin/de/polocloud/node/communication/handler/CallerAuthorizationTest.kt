package de.polocloud.node.communication.handler

import de.polocloud.common.communication.server.context.GrpcServerContext
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CallerAuthorizationTest {

    private fun serviceContext(id: String) = GrpcServerContext().with("serviceSubject", id)
    private fun trustedContext() = GrpcServerContext()

    @Test
    fun `requireTrustedCaller allows a CLI operator or peer node`() {
        CallerAuthorization.requireTrustedCaller(trustedContext(), "do the thing")
    }

    @Test
    fun `requireTrustedCaller rejects a launched service`() {
        assertThrows(IllegalStateException::class.java) {
            CallerAuthorization.requireTrustedCaller(serviceContext("lobby-1"), "do the thing")
        }
    }

    @Test
    fun `requireSelfOrTrustedCaller allows a CLI operator or peer node targeting any service`() {
        CallerAuthorization.requireSelfOrTrustedCaller(trustedContext(), "lobby-1")
        CallerAuthorization.requireSelfOrTrustedCaller(trustedContext(), "proxy-1")
    }

    @Test
    fun `requireSelfOrTrustedCaller allows a service targeting itself`() {
        CallerAuthorization.requireSelfOrTrustedCaller(serviceContext("lobby-1"), "lobby-1")
    }

    @Test
    fun `requireSelfOrTrustedCaller allows a service targeting itself case-insensitively`() {
        CallerAuthorization.requireSelfOrTrustedCaller(serviceContext("Lobby-1"), "lobby-1")
    }

    @Test
    fun `requireSelfOrTrustedCaller rejects a service targeting another service`() {
        assertThrows(IllegalStateException::class.java) {
            CallerAuthorization.requireSelfOrTrustedCaller(serviceContext("lobby-1"), "proxy-1")
        }
    }
}
