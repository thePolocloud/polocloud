package de.polocloud.node.communication.handler.cluster

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.registration.node.token.RegistrationToken
import de.polocloud.proto.CreateTokenRequest
import de.polocloud.proto.NodeState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Covers the authorization check added to [CreateTokenServerHandler]: this RPC is meant
 * for CLI operators onboarding a new node, not for peer nodes calling it on themselves
 * — both share the same mTLS CA on the cluster port (see [de.polocloud.node.communication.grpc.NodeGrpcEndpoint]),
 * so without the check any reachable node could mint its own registration token.
 *
 * Uses a real (throwaway) H2 database — same reasoning as the other cluster tests —
 * since the "is this caller a known node" check goes through [NodeRepository]. Token
 * creation itself is injected, so the success path doesn't need a booted [de.polocloud.node.core.environment.NodeEnvironment].
 */
class CreateTokenServerHandlerTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-createtoken-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            runCatching { TranslationService.init() }
            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
        }
    }

    private fun registeredNode(index: Int) = NodeData(
        id = UUID.randomUUID(),
        nodeIndex = index,
        hostname = "10.0.0.$index",
        port = 4240 + index,
        state = NodeState.ONLINE,
        version = "3",
        gitCommitHash = "abc",
    ).also { NodeRepository.save(it) }

    private fun handler(issued: RegistrationToken = RegistrationToken("issued-token", Long.MAX_VALUE)) =
        CreateTokenServerHandler(createToken = { issued })

    private fun contextWithSubject(subject: String?) =
        subject?.let { GrpcServerContext().with("subject", it) } ?: GrpcServerContext()

    @Test
    fun `a call from a known node's identity is rejected`() {
        val node = registeredNode(1)

        assertThrows(IllegalStateException::class.java) {
            runBlocking { handler().handle(CreateTokenRequest.getDefaultInstance(), contextWithSubject(node.id.toString())) }
        }
    }

    @Test
    fun `a call from a CLI session (non-uuid subject) is allowed`() = runBlocking {
        val issued = RegistrationToken("cli-issued", Long.MAX_VALUE)

        val response = handler(issued).handle(CreateTokenRequest.getDefaultInstance(), contextWithSubject("operator-mirco"))

        assertEquals(issued.token, response.token.token)
    }

    @Test
    fun `a call with no subject at all is allowed`() = runBlocking {
        val issued = RegistrationToken("no-subject-token", Long.MAX_VALUE)

        val response = handler(issued).handle(CreateTokenRequest.getDefaultInstance(), contextWithSubject(null))

        assertEquals(issued.token, response.token.token)
    }

    @Test
    fun `a uuid subject that is not a registered node is allowed`() = runBlocking {
        val issued = RegistrationToken("unregistered-uuid-token", Long.MAX_VALUE)

        val response = handler(issued).handle(CreateTokenRequest.getDefaultInstance(), contextWithSubject(UUID.randomUUID().toString()))

        assertEquals(issued.token, response.token.token)
    }
}
