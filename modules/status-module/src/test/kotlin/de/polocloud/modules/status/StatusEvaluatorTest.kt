package de.polocloud.modules.status

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusEvaluatorTest {

    private fun service(group: String, state: ServiceState, players: Int = 0, maxPlayers: Int = 0) = Service(
        id = "$group-service",
        index = 1,
        group = group,
        state = state,
        port = 25565,
        host = "127.0.0.1",
        pid = 1,
        onlinePlayers = players,
        maxPlayers = maxPlayers,
    )

    @Test
    fun `a group with at least one running service is ONLINE`() {
        val status = StatusEvaluator.evaluate(
            "bedwars",
            StatusGroupConfig(displayName = "BedWars", available = true),
            listOf(service("bedwars", ServiceState.RUNNING, players = 4, maxPlayers = 16)),
        )

        assertEquals(GroupAvailability.ONLINE, status.availability)
        assertEquals(1, status.onlineServices)
        assertEquals(4, status.onlinePlayers)
        assertEquals(16, status.maxPlayers)
    }

    @Test
    fun `a group with no running services is OFFLINE`() {
        val status = StatusEvaluator.evaluate(
            "bedwars",
            StatusGroupConfig(displayName = "BedWars", available = true),
            listOf(service("bedwars", ServiceState.STOPPED), service("bedwars", ServiceState.STARTING)),
        )

        assertEquals(GroupAvailability.OFFLINE, status.availability)
        assertEquals(0, status.onlineServices)
    }

    @Test
    fun `available=false forces MAINTENANCE even with running services`() {
        val status = StatusEvaluator.evaluate(
            "bedwars",
            StatusGroupConfig(displayName = "BedWars", available = false),
            listOf(service("bedwars", ServiceState.RUNNING, players = 4, maxPlayers = 16)),
        )

        assertEquals(GroupAvailability.MAINTENANCE, status.availability)
    }

    @Test
    fun `a blank displayName falls back to the group name`() {
        val status = StatusEvaluator.evaluate("bedwars", StatusGroupConfig(displayName = ""), emptyList())

        assertEquals("bedwars", status.displayName)
    }

    @Test
    fun `only RUNNING services count toward totals`() {
        val status = StatusEvaluator.evaluate(
            "bedwars",
            StatusGroupConfig(available = true),
            listOf(
                service("bedwars", ServiceState.RUNNING, players = 2, maxPlayers = 16),
                service("bedwars", ServiceState.RUNNING, players = 3, maxPlayers = 16),
                service("bedwars", ServiceState.STARTING, players = 0, maxPlayers = 16),
            ),
        )

        assertEquals(2, status.onlineServices)
        assertEquals(5, status.onlinePlayers)
        assertEquals(32, status.maxPlayers)
    }
}
