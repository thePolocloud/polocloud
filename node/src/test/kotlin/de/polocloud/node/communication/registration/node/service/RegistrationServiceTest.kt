package de.polocloud.node.communication.registration.node.service

import de.polocloud.common.communication.generator.certificate.CertificateSigningRequestGenerator
import de.polocloud.common.communication.security.toPem
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.cli.session.CliSessionManager
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.utils.rootDir
import de.polocloud.proto.NodeVersion
import de.polocloud.proto.RegisterNodeRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
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
 * returned CA certificate.
 *
 * Needs a real (throwaway) H2 database — same reasoning as [de.polocloud.node.services.queue.ServiceQueueEligibilityTest] —
 * plus a real [NodeCertificateStorage], since [RegistrationService] talks to both
 * directly rather than through injectable seams. [NodeCertificateStorage] is a global
 * singleton keyed off the JVM-wide `rootDir` system property and only reads it once,
 * on first access: this test must be the only one in the suite that touches it, or
 * whichever test runs first "wins" the directory for the rest of the JVM.
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
}
