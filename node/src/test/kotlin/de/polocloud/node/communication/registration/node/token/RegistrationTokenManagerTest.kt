package de.polocloud.node.communication.registration.node.token

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistrationTokenManagerTest {

    @Test
    fun `a freshly created token validates`() {
        val manager = RegistrationTokenManager()
        val token = manager.create()

        assertTrue(manager.validate(token.token))
    }

    @Test
    fun `a token can only be validated once`() {
        val manager = RegistrationTokenManager()
        val token = manager.create()

        assertTrue(manager.validate(token.token))
        assertFalse(manager.validate(token.token))
    }

    @Test
    fun `an unknown token does not validate`() {
        val manager = RegistrationTokenManager()

        assertFalse(manager.validate("does-not-exist"))
    }

    @Test
    fun `an expired token does not validate`() {
        val manager = RegistrationTokenManager()
        val token = manager.create(ttlMs = -1)

        assertFalse(manager.validate(token.token))
    }

    @Test
    fun `validating an expired token still consumes it`() {
        val manager = RegistrationTokenManager()
        val token = manager.create(ttlMs = -1)

        assertFalse(manager.validate(token.token))
        // Re-validating must not somehow succeed just because the first check removed it.
        assertFalse(manager.validate(token.token))
    }

    @Test
    fun `the initial token has a longer ttl than the default`() {
        val manager = RegistrationTokenManager()
        val default = manager.create()
        val initial = manager.createInitialToken()

        assertTrue(initial.expiresAt - System.currentTimeMillis() > default.expiresAt - System.currentTimeMillis())
    }
}
