package de.polocloud.node.communication.registration.node.service

import de.polocloud.common.Address
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.generator.certificate.CertificateSigningRequestGenerator
import de.polocloud.common.communication.security.toPem
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeFactory
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.cli.session.CliSessionManager
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.grpc.ServiceGrpcEndpoint
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.node.RegistrationInfo
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.group.GroupService
import de.polocloud.node.identity.JoinResult
import de.polocloud.node.identity.NodeIdentityService
import de.polocloud.node.identity.provider.NodeIdProvider
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.utils.rootDir
import de.polocloud.proto.NodeVersion
import de.polocloud.proto.RegisterNodeRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.UUID
import kotlin.io.path.Path
import kotlinx.serialization.json.Json

/**
 * Covers the node join handshake ([RegistrationService.registerNode]): token
 * validation/single-use, version enforcement, duplicate-id rejection, and that a
 * successful join actually persists the node and returns a certificate signed by the
 * returned CA certificate. Also covers the client/live side of that same handshake —
 * [de.polocloud.node.identity.NodeIdentityService.joinLive], the in-process counterpart
 * used by the interactive `cluster join` terminal command (see those tests further down)
 * — since it's the same handshake, just driven from the joining node's side over a real
 * network round trip instead of calling [RegistrationService.registerNode] directly.
 *
 * Needs a real (throwaway) H2 database — same reasoning as [de.polocloud.node.services.queue.ServiceQueueEligibilityTest] —
 * plus a real [NodeCertificateStorage], since [RegistrationService] talks to both
 * directly rather than through injectable seams. [NodeCertificateStorage] is a global
 * singleton keyed off the JVM-wide `rootDir` system property and only reads it once,
 * on first access: this test must be the only one in the suite that touches it, or
 * whichever test runs first "wins" the directory for the rest of the JVM — this is why
 * the `joinLive` tests live here too, rather than in their own file.
 */
class RegistrationServiceTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-registration-test-${UUID.randomUUID()}"
        private val identityDir = "build/tmp/polocloud-registration-identity-${UUID.randomUUID()}"
        private val configPath = "build/tmp/polocloud-registration-config-${UUID.randomUUID()}.json"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            runCatching { TranslationService.init() }

            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }

            rootDir(Path(identityDir))
            NodeCertificateStorage.initialize()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
            File(identityDir).deleteRecursively()
            File(configPath).delete()
        }
    }

    // Re-created per test method (JUnit5's default per-method lifecycle); all instances
    // share configPath, which is fine since none of these tests mutate the config.
    private val configHolder = ConfigurationHolder(
        NodeConfigurations::class,
        configPath,
        Json { ignoreUnknownKeys = true; encodeDefaults = true },
        serializer<NodeConfigurations>(),
    ).apply { value = NodeConfigurations() }

    private val registrationManager = RegistrationManager(
        configHolder,
        CliRegistrationService(configHolder, CliSessionManager()),
    )
    private val service = RegistrationService(registrationManager)

    private fun request(
        localId: UUID = UUID.randomUUID(),
        token: String,
        group: String = "lobby",
        version: String = PolocloudVersion.CURRENT.toString(),
        maxMemory: Int = 2048,
    ): RegisterNodeRequest {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val csrPem = CertificateSigningRequestGenerator(keyPair, localId).generate().toPem()

        return RegisterNodeRequest.newBuilder()
            .setLocalId(localId.toString())
            .setGroup(group)
            .setHostname("10.0.0.${(1..254).random()}")
            .setPort(4240)
            .setCsrPem(csrPem)
            .setDetails(NodeVersion.newBuilder().setVersion(version).setGitHash("abc123").build())
            .setToken(token)
            .setMaxMemory(maxMemory)
            .build()
    }

    private fun parseCert(pem: String): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(pem.toByteArray())) as X509Certificate

    @Test
    fun `accepts a valid registration and persists the node`() = runBlocking {
        val localId = UUID.randomUUID()
        val token = registrationManager.tokenManger.create().token

        val response = service.registerNode(request(localId = localId, token = token, group = "lobby", maxMemory = 4096))

        assertTrue(response.accepted)
        assertTrue(response.certificate.isNotBlank())
        assertTrue(response.caCertificate.isNotBlank())

        val saved = NodeRepository.find(localId)
        assertNotNull(saved)
        assertEquals("lobby", saved?.groupName)
        assertEquals(4096, saved?.maxMemory)
    }

    @Test
    fun `the issued certificate is signed by the returned CA certificate`() = runBlocking {
        val token = registrationManager.tokenManger.create().token

        val response = service.registerNode(request(token = token))

        val cert = parseCert(response.certificate)
        val caCert = parseCert(response.caCertificate)
        cert.verify(caCert.publicKey) // throws SignatureException if the chain doesn't hold
    }

    @Test
    fun `rejects an unknown token`() = runBlocking {
        val response = service.registerNode(request(token = "this-token-was-never-issued"))

        assertFalse(response.accepted)
        assertFalse(response.hasCertificate())
    }

    @Test
    fun `a token can only be used once`() = runBlocking {
        val token = registrationManager.tokenManger.create().token
        val first = service.registerNode(request(token = token))
        val second = service.registerNode(request(token = token))

        assertTrue(first.accepted)
        assertFalse(second.accepted)
    }

    @Test
    fun `rejects a version mismatch`() = runBlocking {
        val token = registrationManager.tokenManger.create().token

        val response = service.registerNode(request(token = token, version = "0.0.0-not-a-real-build"))

        assertFalse(response.accepted)
    }

    @Test
    fun `rejects a node id that is already registered`() = runBlocking {
        val localId = UUID.randomUUID()
        val firstToken = registrationManager.tokenManger.create().token
        service.registerNode(request(localId = localId, token = firstToken))

        val secondToken = registrationManager.tokenManger.create().token
        val response = service.registerNode(request(localId = localId, token = secondToken))

        assertFalse(response.accepted)
    }

    // --- joinLive: the live, in-process client-side path used by `cluster join` -----------

    /**
     * A fully working, manually assembled [NodeRuntimeContext] for a fresh, standalone
     * node — everything [NodeIdentityService.resolve]'s boot path would build, but
     * constructed directly rather than via [NodeIdentityService.resolve] itself, since
     * `resolve`'s "fresh cluster of one" branch can only ever succeed once per shared test
     * database (every later call requires a token, per [NodeRepository.count]). Deliberately
     * does **not** save this node's own [de.polocloud.node.cluster.node.NodeData] into
     * [NodeRepository] — a real standalone node about to run `cluster join` also has no row
     * in the *target* cluster's database yet (only in its own, separate one), which is
     * exactly what a bare `localId` with no prior save reproduces here.
     */
    private class JoinerNode(port: Int, group: String = "lobby") : AutoCloseable {
        val localId: UUID = UUID.randomUUID()
        val address: Address = Address("127.0.0.1", port)

        val configHolder = ConfigurationHolder(
            NodeConfigurations::class,
            "build/tmp/polocloud-joiner-config-${UUID.randomUUID()}.json",
            Json { ignoreUnknownKeys = true; encodeDefaults = true },
            serializer<NodeConfigurations>(),
        ).apply {
            value = NodeConfigurations().apply {
                general.bindAddress = address
                general.apiAddress = Address("127.0.0.1", port + 1)
                general.hostname = "127.0.0.1"
                general.serviceHostname = "127.0.0.1"
                cluster.registration = Address("127.0.0.1", port + 2)
            }
        }

        val container = LocalNodeContainer(
            NodeFactory.create(
                id = localId, index = 1, groupName = group, address = address,
                version = PolocloudVersion.CURRENT.toString(), gitCommitHash = "abc123", head = true,
            )
        )

        val serviceProvider = ServiceProvider(nodePort = port + 1, nodeHost = "127.0.0.1", nodeId = localId.toString())
        val groupService = GroupService(serviceProvider.platformService)
        val cliSessionManager = CliSessionManager()
        val cliRegistrationService = CliRegistrationService(configHolder, cliSessionManager)
        val electionService = NodeElectionService()
        val registrationManager = RegistrationManager(configHolder, cliRegistrationService)

        val identityService = NodeIdentityService(
            nodeId = object : NodeIdProvider { override fun get() = localId },
            holder = configHolder,
            registrationManager = registrationManager,
            cliRegistrationService = cliRegistrationService,
            cliSessionManager = cliSessionManager,
            electionService = electionService,
        )

        val context: NodeRuntimeContext

        init {
            val grpc = NodeGrpcEndpoint(address, cliRegistrationService, cliSessionManager, groupService, serviceProvider, electionService)
            val serviceGrpc = ServiceGrpcEndpoint(Address("127.0.0.1", port + 1), groupService, serviceProvider)
            grpc.start()
            serviceGrpc.start()
            context = NodeRuntimeContext(
                configHolder, container, registrationManager, grpc, serviceGrpc,
                groupService, serviceProvider, electionService, identityService,
            )
        }

        override fun close() {
            context.grpcEndpoint.close(ShutdownMode.GRACEFUL)
            context.serviceGrpcEndpoint.close(ShutdownMode.GRACEFUL)
            registrationManager.close(ShutdownMode.GRACEFUL)
            context.cli.shutdown()
        }
    }

    /** A lightweight, reachable acceptor — just enough to answer [RegistrationClient] calls. */
    private class Acceptor(port: Int) : AutoCloseable {
        val configHolder = ConfigurationHolder(
            NodeConfigurations::class,
            "build/tmp/polocloud-acceptor-config-${UUID.randomUUID()}.json",
            Json { ignoreUnknownKeys = true; encodeDefaults = true },
            serializer<NodeConfigurations>(),
        ).apply { value = NodeConfigurations().apply { cluster.registration = Address("127.0.0.1", port) } }

        val registrationManager = RegistrationManager(configHolder, CliRegistrationService(configHolder, CliSessionManager()))

        init {
            registrationManager.allowRequests()
        }

        override fun close() = registrationManager.close(ShutdownMode.GRACEFUL)
    }

    private fun isReachable(address: Address): Boolean =
        try {
            Socket().use { it.connect(InetSocketAddress(address.hostname, address.port), 2000) }
            true
        } catch (_: Exception) {
            false
        }

    @Test
    fun `joinLive is denied for an invalid token and leaves the running endpoints untouched`() {
        // Deliberately reachable (a real Acceptor), just with a token it never issued: an
        // *unreachable* target is a different, pre-existing failure mode entirely — see
        // RegistrationClient.tryRegister, which throws rather than returning a rejection
        // for transport-level failures — and is already guarded against for the real
        // interactive flow by ClusterJoinWizard's own reachability pre-check.
        Acceptor(port = 39411).use { acceptor ->
            JoinerNode(port = 39421).use { joiner ->
                val oldGrpc = joiner.context.grpcEndpoint
                val oldServiceGrpc = joiner.context.serviceGrpcEndpoint

                val result = joiner.identityService.joinLive(
                    RegistrationInfo("this-token-was-never-issued", Address("127.0.0.1", 39411)),
                    "lobby",
                    joiner.context,
                )

                assertTrue(result is JoinResult.Denied, "expected Denied but was $result")
                // A denied join must not touch the running endpoints or this node's identity.
                assertSame(oldGrpc, joiner.context.grpcEndpoint)
                assertSame(oldServiceGrpc, joiner.context.serviceGrpcEndpoint)
                assertEquals(1, joiner.context.localNodeContainer.data.nodeIndex)
            }
        }
    }

    @Test
    fun `joinLive is Unreachable rather than throwing when the target refuses the connection`() {
        JoinerNode(port = 39431).use { joiner ->
            val oldGrpc = joiner.context.grpcEndpoint
            val oldServiceGrpc = joiner.context.serviceGrpcEndpoint

            // Nothing is listening on this port — a plain connection failure, distinct
            // from a real Acceptor rejecting the token (see the Denied test above).
            val result = joiner.identityService.joinLive(
                RegistrationInfo("irrelevant-token", Address("127.0.0.1", 39432)),
                "lobby",
                joiner.context,
            )

            assertTrue(result is JoinResult.Unreachable, "expected Unreachable but was $result")
            assertSame(oldGrpc, joiner.context.grpcEndpoint)
            assertSame(oldServiceGrpc, joiner.context.serviceGrpcEndpoint)
        }
    }

    @Test
    fun `joinLive rewires this node into the cluster and rebuilds its gRPC endpoints in place`() {
        Acceptor(port = 39511).use { acceptor ->
            JoinerNode(port = 39521).use { joiner ->
                val oldGrpc = joiner.context.grpcEndpoint
                val oldServiceGrpc = joiner.context.serviceGrpcEndpoint
                val token = acceptor.registrationManager.tokenManger.create().token

                val result = joiner.identityService.joinLive(
                    RegistrationInfo(token, Address("127.0.0.1", 39511)),
                    "lobby",
                    joiner.context,
                )

                assertTrue(result is JoinResult.Success, "expected Success but was $result")
                val nodeData = (result as JoinResult.Success).nodeData

                // The cluster (not this node) assigns nodeIndex on registration — see
                // RegistrationService.registerNode / IndexGenerator.generateNode.
                assertNotNull(NodeRepository.find(joiner.localId))
                assertEquals(nodeData.nodeIndex, joiner.context.localNodeContainer.data.nodeIndex)

                // The old endpoints' mTLS material is now stale (self-signed bootstrap CA,
                // not the cluster's) — joinLive must have swapped in fresh instances...
                assertNotSame(oldGrpc, joiner.context.grpcEndpoint)
                assertNotSame(oldServiceGrpc, joiner.context.serviceGrpcEndpoint)
                // ...bound on the very same address the old ones were, reachable again.
                assertTrue(isReachable(joiner.address))
            }
        }
    }
}
