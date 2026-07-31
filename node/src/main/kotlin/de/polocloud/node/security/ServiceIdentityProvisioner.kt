package de.polocloud.node.security

import de.polocloud.common.Address
import de.polocloud.common.communication.certificate.certToPem
import de.polocloud.common.communication.security.toPem
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.proto.RegisterServiceRequest
import de.polocloud.proto.ServiceRegistrationServiceGrpcKt
import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator

/**
 * Provisions the mTLS identity a locally launched service needs to talk back to
 * the node via the standalone API.
 *
 * Certificate issuance is still centralized on the head by policy — every node does end
 * up holding a copy of the real cluster CA key pair (see [NodeCertificateStorage]'s doc
 * comment / [NodeCertificateStorage.adoptClusterCaKeyPair], needed so leader election can
 * promote any node to head), but a non-head node forwards the CSR to the current head over
 * [de.polocloud.node.communication.impl.services.ServiceRegistrationServiceImpl] rather
 * than signing locally, so it never depends on this node's own copy actually being
 * up to date. Only the head signs directly. That mismatch (a node self-signing with a CA
 * key that didn't match the certificate the rest of the cluster trusted) previously made
 * every service launched on a
 * non-head node fail its first mTLS handshake with a generic `TLSV1_ALERT_INTERNAL_ERROR`).
 *
 * Either way the PEM files end up laid out in the directory layout the API's
 * [de.polocloud.api.connection.ServiceCertificateStorage] expects:
 *
 * ```
 * <identity-dir>/
 *   private-key.pem
 *   public-key.pem
 *   certificate.pem   ← signed by the cluster CA
 *   ca.pem            ← cluster CA certificate
 * ```
 *
 * The identity directory is then handed to the process via the
 * `POLOCLOUD_IDENTITY_DIR` environment variable so the API picks it up on its
 * first call.
 */
object ServiceIdentityProvisioner {

    /**
     * Generates and writes a freshly signed identity for the given service into
     * [identityDir]. Any previously provisioned files are overwritten so a
     * restarted service always receives a valid certificate.
     *
     * @param identityDir target directory for the PEM files (created if missing)
     * @param serviceId   unique id of the service instance (used as cert CN + SAN)
     * @param planName    group / plan name the service belongs to (added as SAN)
     */
    fun provision(identityDir: File, serviceId: String, planName: String) {
        identityDir.mkdirs()

        val keyPair = generateKeyPair()
        val csr = buildCsr(keyPair, serviceId)
        val (certificatePem, caCertificatePem) = if (isHead()) {
            val ca = NodeCertificateStorage.certificateAuthority()
            val signed = ca.signCsr(csr, subjectAltNames = SanBuilder.forService(serviceId, planName))
            certToPem(signed) to ca.getCaCertificatePem()
        } else {
            signViaHead(csr, serviceId, planName)
        }

        writePem(File(identityDir, "private-key.pem"), keyPair.private)
        writePem(File(identityDir, "public-key.pem"), keyPair.public)
        File(identityDir, "certificate.pem").writeText(certificatePem)
        File(identityDir, "ca.pem").writeText(caCertificatePem)
    }

    // Live in-memory Raft role, not the NodeRepository.head DB projection — that column
    // is only a best-effort mirror of whoever last won an election and can lag or (absent
    // the RPC payload/caller-identity check in NodeServiceImpl) be forged by another
    // cluster member, whereas this node's own belief about its role is authoritative for
    // deciding whether *it* should sign locally.
    private fun isHead(): Boolean = NodeEnvironment.runtime.electionService.isHead()

    /** Forwards [csr] to whichever node is currently head and returns (certificate, CA) PEMs. */
    private fun signViaHead(csr: PKCS10CertificationRequest, serviceId: String, planName: String): Pair<String, String> {
        val head = NodeRepository.findAll().firstOrNull { it.head }
            ?: error("Cannot provision identity for service '$serviceId' — no cluster head is currently known")

        val client = NodeGrpcClient()
        client.connect(Address(head.hostname, head.port))
        try {
            val stub = ServiceRegistrationServiceGrpcKt.ServiceRegistrationServiceCoroutineStub(client.channel())
            val response = runBlocking {
                stub.registerService(
                    RegisterServiceRequest.newBuilder()
                        .setServiceId(serviceId)
                        .setPlanName(planName)
                        .setCsrPem(csr.toPem())
                        .build()
                )
            }
            if (!response.accepted) {
                error("Head node '${head.name()}' refused to sign service '$serviceId': ${response.message}")
            }
            return response.certificate to response.caCertificate
        } finally {
            client.disconnect()
        }
    }

    private fun buildCsr(keyPair: KeyPair, serviceId: String): PKCS10CertificationRequest {
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        return JcaPKCS10CertificationRequestBuilder(X500Name("CN=$serviceId"), keyPair.public)
            .build(signer)
    }

    private fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    private fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }
}