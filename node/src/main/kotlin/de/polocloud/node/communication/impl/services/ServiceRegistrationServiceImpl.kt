package de.polocloud.node.communication.impl.services

import de.polocloud.common.communication.certificate.certToPem
import de.polocloud.common.communication.certificate.parseCsr
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.node.security.SanBuilder
import de.polocloud.proto.RegisterServiceRequest
import de.polocloud.proto.RegisterServiceResponse
import de.polocloud.proto.ServiceRegistrationServiceGrpcKt
import org.slf4j.LoggerFactory

/**
 * Signs the mTLS identity of a service launched on some *other* node in the cluster.
 *
 * Only the elected head holds the cluster CA's private key (see
 * [NodeCertificateStorage.caKeyPair] / the doc comment on [NodeCertificateStorage]) — every
 * other node only has the CA *certificate*, received during its own registration. A
 * non-head node therefore cannot sign a service certificate itself: it forwards the CSR to
 * whichever node is currently head over this RPC instead of self-signing with a CA key that
 * doesn't match the certificate everyone else trusts. See
 * [de.polocloud.node.security.ServiceIdentityProvisioner] for the calling side.
 */
class ServiceRegistrationServiceImpl : ServiceRegistrationServiceGrpcKt.ServiceRegistrationServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(ServiceRegistrationServiceImpl::class.java)

    override suspend fun registerService(request: RegisterServiceRequest): RegisterServiceResponse {
        // Live in-memory Raft role, not the NodeRepository.head DB projection — see
        // ServiceIdentityProvisioner.isHead for why that column alone isn't trustworthy
        // enough to gate signing on.
        if (!NodeEnvironment.runtime.electionService.isHead()) {
            return deny("This node is not the cluster head and cannot sign service certificates.")
        }

        val signed = runCatching {
            val ca = NodeCertificateStorage.certificateAuthority()
            val csr = parseCsr(request.csrPem)
            val cert = ca.signCsr(csr, subjectAltNames = SanBuilder.forService(request.serviceId, request.planName))
            certToPem(cert) to ca.getCaCertificatePem()
        }.getOrElse { ex ->
            logger.warn("Failed to sign service certificate for '{}'", request.serviceId, ex)
            return deny("Failed to sign certificate: ${ex.message}")
        }

        return RegisterServiceResponse.newBuilder()
            .setAccepted(true)
            .setCertificate(signed.first)
            .setCaCertificate(signed.second)
            .build()
    }

    private fun deny(message: String): RegisterServiceResponse =
        RegisterServiceResponse.newBuilder()
            .setAccepted(false)
            .setMessage(message)
            .build()
}