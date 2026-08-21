package de.polocloud.cli.communication.connection

import de.polocloud.common.Address
import de.polocloud.i18n.api.TranslationService
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.DisconnectRequest
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Orchestrates the full CLI → cluster connection lifecycle.
 *
 * Port responsibilities:
 * - [registrationAddress] (e.g. 4239) — plaintext, one-time registration (token + CSR → signed cert)
 * - [clusterAddress]      (e.g. 4240) — mTLS, all subsequent cluster communication
 *
 * Usage:
 * ```kotlin
 * val connection = CliConnectionManager()
 * connection.connect(
 *     clusterAddress      = Address("localhost", 4240),
 *     registrationAddress = Address("localhost", 4239),
 *     token               = "my-token"
 * )
 * val stub = MyServiceGrpcKt.MyServiceCoroutineStub(connection.channel())
 * ```
 */
class CliConnectionManager(
    val certificateStorage: CliCertificateStorage,
    private val registrationClient: CliRegistrationClient = CliRegistrationClient(certificateStorage),
    private val grpcChannel: CliGrpcChannel = CliGrpcChannel(certificateStorage),
) : CliConnection {

    private val logger = LoggerFactory.getLogger(CliConnectionManager::class.java)

    override val isConnected: Boolean
        get() = grpcChannel.isConnected

    /**
     * Connects to the cluster.
     *
     * - If not registered → runs registration on [registrationAddress] (plaintext)
     * - Opens the mTLS channel on [clusterAddress]
     *
     * No-op if already connected.
     */
    override suspend fun connect(
        clusterAddress: Address,
        registrationAddress: Address,
        token: String?
    ) {
        if (isConnected) {
            logger.debug("connect() ignored — already connected")
            return
        }

        if (!certificateStorage.isRegistered() && token != null) {
            logger.info(TranslationService.tr(
                "cli",
                "cli.connect.registration.start",
                "address" to registrationAddress.toString()
            ))

            registrationClient.register(registrationAddress, token)
        } else {
            logger.info(TranslationService.tr("cli", "cli.connect.registration.skip"))
        }

        grpcChannel.connect(clusterAddress)
    }


    override fun channel(): ManagedChannel = grpcChannel.channel()

    fun isRegistered(): Boolean = certificateStorage.isRegistered()

    override fun disconnect() {
        if (!grpcChannel.isConnected) {
            return
        }

        try {
            val state = channel().getState(false)

            if (state == ConnectivityState.READY) {
                val stub = CliRegistrationServiceGrpcKt
                    .CliRegistrationServiceCoroutineStub(channel())

                runBlocking {
                    stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                        .disconnectCli(DisconnectRequest.newBuilder().build())
                }
            }

        } catch (_: Exception) {

        }

        grpcChannel.close()
        logger.info(TranslationService.tr("cli", "cli.connect.disconnected"))
    }

    /**
     * Forces re-registration on the next [connect] call.
     * Useful when the cluster's CLI CA has rotated or the token changed.
     */
    fun forceReregistration() {
        disconnect()
        certificateStorage.clearCertificates()
        logger.info(TranslationService.tr("cli", "cli.connect.reregistration.forced"))
    }
}