package de.polocloud.cli.configuration.connection

import de.polocloud.common.Address
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator

class ConnectionHistoryTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()

    private val history = ConnectionHistory(keyPair, maxEntries = 3)

    @BeforeEach
    fun setUp() {
        history.clear()
    }

    @AfterEach
    fun tearDown() {
        history.clear()
    }

    @Test
    fun `push trims oldest entries beyond maxEntries`() {
        repeat(5) { index ->
            history.push(
                ConnectionEntry(
                    clusterAddress = Address("127.0.0.1", 1000 + index),
                    registrationAddress = Address("127.0.0.1", 2000 + index),
                ),
            )
        }

        val all = history.all()
        assertEquals(3, all.size)
        assertEquals(1004, all[0].clusterAddress.port)
        assertEquals(1003, all[1].clusterAddress.port)
        assertEquals(1002, all[2].clusterAddress.port)
    }

    @Test
    fun `push replaces existing entry for the same cluster address`() {
        val address = Address("127.0.0.1", 25565)
        history.push(
            ConnectionEntry(
                clusterAddress = address,
                registrationAddress = Address("127.0.0.1", 1),
                lastConnected = 1L,
            ),
        )
        history.push(
            ConnectionEntry(
                clusterAddress = address,
                registrationAddress = Address("127.0.0.1", 2),
                lastConnected = 2L,
            ),
        )

        val all = history.all()
        assertEquals(1, all.size)
        assertEquals(2, all.single().registrationAddress.port)
        assertEquals(2L, all.single().lastConnected)
    }
}
