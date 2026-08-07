package de.polocloud.common.communication.certificate

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security

/**
 * Abstract base for all certificate/key storage implementations.
 *
 * Handles the common mechanics of:
 * - creating the storage directory
 * - generating or loading an RSA-2048 key pair
 * - reading and writing PEM files
 * - tracking registration state (cert + CA on disk)
 *
 * Subclasses declare only **where** their files live by overriding
 * [storageDir], [certificateFile], [privateKeyFile], [publicKeyFile],
 * and [caCertificateFile].
 *
 * ```
 *                    CertificateStorage  (common)
 *                    /          |         \
 *     NodeCertStorage   CliCertStorage   ServiceCertStorage
 *     (node module)     (cli module)     (service-sdk module)
 * ```
 *
 * Registration flow shared by all three:
 * 1. `isRegistered()` → false on first start
 * 2. Generate CSR from [keyPair]
 * 3. Send CSR to the appropriate registration endpoint
 * 4. Call [saveCertificates] with the signed cert + CA cert
 * 5. `isRegistered()` → true on all subsequent starts
 */
abstract class CertificateStorage {

    /** Root directory where all PEM files for this identity are stored. */
    protected abstract val storageDir: Path

    /** The signed certificate PEM file for this identity. */
    abstract fun certificateFile(): File

    /** The private key PEM file for this identity. */
    abstract fun privateKeyFile(): File

    /** The CA certificate PEM file used to verify the other party. */
    abstract fun caCertificateFile(): File

    /** The public key PEM file (persisted alongside the private key). */
    protected abstract fun publicKeyFile(): File

    /**
     * The RSA key pair for this identity.
     * Loaded from disk if it already exists, generated and persisted otherwise.
     * Available after [initialize] has been called.
     */
    lateinit var keyPair: KeyPair
        private set

    /**
     * Initializes the storage: ensures the directory exists, registers
     * the BouncyCastle provider, and loads or generates the key pair.
     *
     * Call this once during module startup before accessing any other member.
     */
    fun initialize() {
        ensureBouncyCastle()
        Files.createDirectories(storageDir)
        restrictDirToOwnerOnly(storageDir)
        keyPair = loadOrCreateKeyPair()
        onInitialized()
    }

    /**
     * Hook called at the end of [initialize].
     * Subclasses can override to perform additional setup (e.g. bootstrapping
     * a self-signed CA certificate like [NodeCertificateStorage] does).
     */
    protected open fun onInitialized() {}

    /**
     * Returns true when both the signed certificate and the CA certificate
     * are present on disk — i.e. registration has been completed at least once.
     */
    fun isRegistered(): Boolean =
        certificateFile().exists() && caCertificateFile().exists()

    /**
     * Persists the signed certificate and CA certificate received from the
     * registration endpoint.
     *
     * @param certPem   PEM-encoded signed certificate for this identity
     * @param caCertPem PEM-encoded CA certificate to trust
     */
    fun saveCertificates(certPem: String, caCertPem: String) {
        certificateFile().writeText(certPem)
        caCertificateFile().writeText(caCertPem)
    }

    /**
     * Deletes the certificate and CA certificate, forcing re-registration on
     * the next [initialize] cycle.
     */
    fun clearCertificates() {
        certificateFile().delete()
        caCertificateFile().delete()
    }

    protected fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }

    protected fun loadKeyPairFromPem(privateFile: File, publicFile: File): KeyPair {
        val converter = JcaPEMKeyConverter().setProvider("BC")

        val privateKey = PEMParser(FileReader(privateFile)).use { parser ->
            when (val obj = parser.readObject()) {
                is PEMKeyPair -> converter.getKeyPair(obj).private
                else -> converter.getPrivateKey(
                    org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(obj)
                )
            }
        }

        val publicKey = PEMParser(FileReader(publicFile)).use { parser ->
            converter.getPublicKey(
                org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(parser.readObject())
            )
        }

        return KeyPair(publicKey, privateKey)
    }

    private fun loadOrCreateKeyPair(): KeyPair {
        val priv = privateKeyFile()
        val pub  = publicKeyFile()

        return if (priv.exists() && pub.exists()) {
            loadKeyPairFromPem(priv, pub)
        } else {
            val kp = generateKeyPair()
            writePem(priv, kp.private)
            restrictToOwnerOnly(priv)
            writePem(pub,  kp.public)
            kp
        }
    }

    private fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    private fun ensureBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
}