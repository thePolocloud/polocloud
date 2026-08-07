package de.polocloud.node.security

import de.polocloud.common.communication.certificate.CertificateStorage
import de.polocloud.common.communication.certificate.restrictDirToOwnerOnly
import de.polocloud.common.communication.certificate.restrictToOwnerOnly
import de.polocloud.node.utils.rootDir
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileReader
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Certificate storage for a **node** identity.
 *
 * Directory layout under `.cache/identity/`:
 * ```
 * .cache/identity/
 *   node/
 *     private-key.pem   ← node private key
 *     public-key.pem
 *     certificate.pem   ← signed by CA (written after registration or bootstrapped)
 *   ca/
 *     private-key.pem   ← cluster CA private key
 *     public-key.pem
 *     certificate.pem   ← CA self-signed cert
 * ```
 *
 * On first start [onInitialized] bootstraps the CA and a node certificate so
 * the node is immediately operational even before any other node joins.
 * Joining nodes overwrite `node/certificate.pem` and `ca/certificate.pem`
 * via [saveCertificates] once the registration handshake completes, then adopt the
 * *real* `ca/private-key.pem` from the current head via [adoptClusterCaKeyPair] — the
 * one [onInitialized] generated locally in the meantime was only ever a throwaway used
 * to self-sign this node's own bootstrap certificate before it had joined anything.
 * Every node ends up holding the real CA key pair so leader election (see
 * `NodeElectionService`) can promote any of them to head without breaking certificate
 * issuance for the rest of the cluster.
 */
object NodeCertificateStorage : CertificateStorage() {

    private val basePath = rootDir().resolve(".cache/identity")
    private val nodePath = basePath.resolve("node")
    private val caPath = basePath.resolve("ca")

    // node identity
    override val storageDir: Path get() = nodePath
    override fun certificateFile(): File = nodePath.resolve("certificate.pem").toFile()
    override fun privateKeyFile(): File = nodePath.resolve("private-key.pem").toFile()
    override fun publicKeyFile(): File = nodePath.resolve("public-key.pem").toFile()
    override fun caCertificateFile(): File = caPath.resolve("certificate.pem").toFile()

    // CA key pair — loaded by initialize() via the base class logic for the CA path
    private val caPrivateKeyFile get() = caPath.resolve("private-key.pem").toFile()
    private val caPublicKeyFile  get() = caPath.resolve("public-key.pem").toFile()

    lateinit var caKeyPair: java.security.KeyPair
        private set

    /** Node ID used when building SANs for the bootstrap certificate. */
    var nodeId: String = ""

    /** `general.hostname`, added to the bootstrap certificate's SAN — see [NodeIdentityPolicy]. */
    var configuredHostname: String? = null

    /**
     * Called by the base class at the end of [initialize].
     * Loads or generates the CA key pair and bootstraps certificates if needed.
     */
    override fun onInitialized() {
        Files.createDirectories(caPath)
        restrictDirToOwnerOnly(caPath)
        caKeyPair = loadOrCreateCaKeyPair()

        if (!isRegistered()) {
            bootstrap()
        }
    }

    fun certificateAuthority(): CertificateAuthority =
        CertificateAuthority(caKeyPair, loadCaCertificate())

    /**
     * Replaces the local CA key pair with the cluster's real one, received from a peer
     * that already holds it (see `NodeService.FetchClusterCa`).
     *
     * A joining node's own [onInitialized] always generates a throwaway CA key pair for
     * its own bootstrap before it has even attempted registration — fine for a node that
     * stays a regular member, but useless the moment leader election (see
     * `NodeElectionService`) promotes this node to head: signing with that throwaway key
     * would produce certificates nothing else in the cluster trusts, since only the
     * *certificate* (not the key) it received via [saveCertificates] is the real one.
     * Every node adopts the real key pair right after joining so any of them can safely
     * become head later.
     */
    fun adoptClusterCaKeyPair(privateKeyPem: String, publicKeyPem: String) {
        Files.createDirectories(caPath)
        restrictDirToOwnerOnly(caPath)
        caPrivateKeyFile.writeText(privateKeyPem)
        restrictToOwnerOnly(caPrivateKeyFile)
        caPublicKeyFile.writeText(publicKeyPem)
        caKeyPair = loadKeyPairFromPem(caPrivateKeyFile, caPublicKeyFile)
    }

    private fun loadCaCertificate(): X509Certificate {
        PEMParser(FileReader(caCertificateFile())).use { parser ->
            val obj = parser.readObject()
            return JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(obj as X509CertificateHolder)
        }
    }

    private fun bootstrap() {
        val caCert = generateCertificate(
            subject = "CN=Polocloud-Root-CA",
            keyPair = caKeyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA",
            isCa = true,
        )

        val nodeCert = generateCertificate(
            subject = "CN=Polocloud-Node",
            keyPair = keyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA",
            isCa = false,
        )

        writePem(caCertificateFile(), caCert)
        writePem(certificateFile(), nodeCert)
    }

    private fun generateCertificate(
        subject: String,
        keyPair: java.security.KeyPair,
        issuerKeyPair: java.security.KeyPair,
        issuer: String,
        isCa: Boolean,
    ): X509Certificate {
        val now   = Date()
        val until = Date(now.time + 3650L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            X500Name(issuer),
            BigInteger(64, SecureRandom()),
            now,
            until,
            X500Name(subject),
            keyPair.public,
        )

        if (isCa) {
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            builder.addExtension(Extension.keyUsage, true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
        } else {
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            builder.addExtension(Extension.keyUsage, true,
                KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment))

            if (nodeId.isNotBlank()) {
                val spec = NodeIdentityPolicy.resolve(nodeId, configuredHostname)
                builder.addExtension(Extension.subjectAlternativeName, false, SanBuilder.build(spec))
            }
        }

        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(issuerKeyPair.private)
        val holder = builder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(holder)
    }

    private fun loadOrCreateCaKeyPair(): java.security.KeyPair {
        return if (caPrivateKeyFile.exists() && caPublicKeyFile.exists()) {
            loadKeyPairFromPem(caPrivateKeyFile, caPublicKeyFile)
        } else {
            Files.createDirectories(caPath)
            restrictDirToOwnerOnly(caPath)
            val kp = KeyPairGenerator.getInstance("RSA")
                .apply { initialize(2048) }
                .generateKeyPair()
            writePem(caPrivateKeyFile, kp.private)
            restrictToOwnerOnly(caPrivateKeyFile)
            writePem(caPublicKeyFile, kp.public)
            kp
        }
    }
}