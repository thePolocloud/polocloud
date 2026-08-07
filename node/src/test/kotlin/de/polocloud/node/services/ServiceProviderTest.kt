package de.polocloud.node.services

import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID

class ServiceProviderTest {

    private fun provider() = ServiceProvider()

    private fun local(group: String, index: Int) =
        LocalService(Service(UUID.randomUUID(), index, group, ServiceState.RUNNING, "127.0.0.1", 30000 + index, ""))

    @Test
    fun `findLocal resolves a running service by its cluster name, case-insensitively`() {
        val provider = provider()
        val lobby = local("lobby", 1)
        provider.localServices += lobby
        provider.localServices += local("proxy", 1)

        assertSame(lobby, provider.findLocal("lobby-1"))
        assertSame(lobby, provider.findLocal("LOBBY-1"))
    }

    @Test
    fun `findLocal returns null when no running service matches`() {
        val provider = provider()
        provider.localServices += local("lobby", 1)
        assertNull(provider.findLocal("survival-9"))
    }

    @Test
    fun `shutdownLocal removes the service from the live list`() {
        val provider = provider()
        val lobby = local("lobby", 1)
        provider.localServices += lobby

        provider.shutdownLocal(lobby)

        assertFalse(provider.localServices.contains(lobby))
        assertEquals(0, provider.localServices.size)
    }

    @Test
    fun `shutdownGroup stops only the targeted group's services`() {
        val provider = provider()
        val lobby1 = local("lobby", 1)
        val lobby2 = local("lobby", 2)
        val proxy = local("proxy", 1)
        provider.localServices += listOf(lobby1, lobby2, proxy)

        provider.shutdownGroup("lobby")

        assertFalse(provider.localServices.contains(lobby1))
        assertFalse(provider.localServices.contains(lobby2))
        assertEquals(listOf(proxy), provider.localServices.toList())
    }

    @Test
    fun `nodeId is exposed as configured`() {
        assertEquals("node-42", ServiceProvider(nodeId = "node-42").nodeId)
    }
}
