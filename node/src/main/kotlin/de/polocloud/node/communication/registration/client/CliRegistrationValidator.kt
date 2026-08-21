package de.polocloud.node.communication.registration.client

import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.node.core.configuration.NodeConfigurations
import java.security.MessageDigest

/**
 * Validates incoming CLI registration requests against the cluster configuration.
 *
 * Extracted from [de.polocloud.node.communication.registration.cli.CliRegistrationService] to isolate validation logic
 * and make it independently testable without a full gRPC context.
 */
class CliRegistrationValidator(
    val holder: ConfigurationHolder<NodeConfigurations>
) {

    sealed interface Result {
        data object Ok : Result
        data class Denied(val translationKey: String) : Result
    }

    fun validateAccess(): Result {
        if (!holder.value.cluster.cliAccess.enabled) return Result.Denied("cli.access.disabled")
        return Result.Ok
    }

    fun validateToken(token: String): Result {
        // Constant-time comparison — this token is long-lived (unlike the single-use node
        // join tokens), so a naive `!=`/`equals` short-circuit would leak a timing side
        // channel on every failed attempt.
        val expected = holder.value.cluster.cliAccess.registrationToken
        val matches = MessageDigest.isEqual(token.toByteArray(), expected.toByteArray())
        if (!matches) return Result.Denied("cluster.registration.cli.token.invalid")
        return Result.Ok
    }
}