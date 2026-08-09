package de.polocloud.cli.configuration.connection

import de.polocloud.common.Address
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.security.KeyPairGenerator

/**
 * Covers the real CLI flows around [ConnectionHistory]:
 * - [de.polocloud.cli.command.impl.cluster.ConnectCommand] pushes after a successful connect
 * - [de.polocloud.cli.communication.connection.auto.AutoConnectService] reads [ConnectionHistory.latest] on boot
 * - history is encrypted on disk and must survive CLI restarts with the same identity key pair
 */
class ConnectionHistoryTest {

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()

    private fun history(maxEntries: Int = 3): ConnectionHistory =
        ConnectionHistory(keyPair, maxEntries = maxEntries)

    private fun entry(port: Int, registrationPort: Int = port + 1000, lastConnected: Long = port.toLong()) =
        ConnectionEntry(
            clusterAddress = Address("127.0.0.1", port),
            registrationAddress = Address("127.0.0.1", registrationPort),
            lastConnected = lastConnected,
        )

    @BeforeEach
    fun setUp() {
        history().clear()
    }

    @AfterEach
    fun tearDown() {
        history().clear()
    }

    @Test
    fun `push trims oldest entries beyond maxEntries`() {
        val history = history(maxEntries = 3)
        repeat(5) { index -> history.push(entry(1000 + index)) }

        val all = history.all()
        assertEquals(3, all.size)
        assertEquals(listOf(1004, 1003, 1002), all.map { it.clusterAddress.port })
    }

    @Test
    fun `push replaces existing entry for the same cluster address`() {
        val history = history()
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

    @Test
    fun `reconnect moves existing cluster to latest without growing past maxEntries`() {
        // Real ConnectCommand flow: reconnecting the same host updates history and becomes auto-connect target.
        val history = history(maxEntries = 3)
        history.push(entry(1001))
        history.push(entry(1002))
        history.push(entry(1003))
        history.push(entry(1001, registrationPort = 4240, lastConnected = 99L))

        val all = history.all()
        assertEquals(3, all.size)
        assertEquals(1001, all[0].clusterAddress.port)
        assertEquals(4240, all[0].registrationAddress.port)
        assertEquals(99L, all[0].lastConnected)
        assertEquals(listOf(1001, 1003, 1002), all.map { it.clusterAddress.port })
        assertEquals(1001, history.latest()?.clusterAddress?.port)
    }

    @Test
    fun `latest returns null when history is empty`() {
        assertNull(history().latest())
        assertEquals(emptyList<ConnectionEntry>(), history().all())
    }

    @Test
    fun `latest returns most recently pushed entry for AutoConnectService`() {
        val history = history()
        history.push(entry(1001))
        history.push(entry(1002))

        val latest = history.latest()
        assertEquals(1002, latest?.clusterAddress?.port)
        assertEquals(2002, latest?.registrationAddress?.port)
    }

    @Test
    fun `history persists across new ConnectionHistory instances with the same key pair`() {
        // Simulates CLI restart: ConnectCommand wrote history, AutoConnectService reads it later.
        history().push(entry(4240, registrationPort = 4239, lastConnected = 10L))
        history().push(entry(25565, registrationPort = 25566, lastConnected = 20L))

        val reloaded = ConnectionHistory(keyPair, maxEntries = 10)
        assertEquals(2, reloaded.all().size)
        assertEquals(25565, reloaded.latest()?.clusterAddress?.port)
        assertEquals(4239, reloaded.all()[1].registrationAddress.port)
    }

    @Test
    fun `clear removes persisted history used by auto-connect`() {
        val history = history()
        history.push(entry(1001))
        history.clear()

        assertNull(history.latest())
        assertEquals(emptyList<ConnectionEntry>(), ConnectionHistory(keyPair).all())
    }
}
