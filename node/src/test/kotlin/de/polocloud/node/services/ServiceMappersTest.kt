package de.polocloud.node.services

import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ServiceMappersTest {

    private fun local(
        state: ServiceState = ServiceState.RUNNING,
        index: Int = 1,
        group: String = "lobby",
        port: Int = 30000,
    ): LocalService {
        val service = LocalService(Service(UUID.randomUUID(), index, group, state, "10.0.0.5", port))
        service.properties["fallback"] = "true"
        return service
    }

    @Test
    fun `player counts default to zero until a ping has landed`() {
        val service = local()
        assertEquals(0, ServiceProtoMapper.toProto(service).onlinePlayers)
        assertEquals(0, ServiceProtoMapper.toProto(service).maxPlayers)
    }

    @Test
    fun `mappers carry player counts set by the ping loop`() {
        val service = local()
        service.onlinePlayers = 7
        service.maxPlayers = 20

        assertEquals(7, ServiceProtoMapper.toProto(service).onlinePlayers)
        assertEquals(20, ServiceProtoMapper.toProto(service).maxPlayers)

        assertEquals(7, ServiceProcessProtoMapper.toProto(service).onlinePlayers)
        assertEquals(20, ServiceProcessProtoMapper.toProto(service).maxPlayers)

        val shared = ServiceEventMapper.toShared(service)
        assertEquals(7, shared.onlinePlayers)
        assertEquals(20, shared.maxPlayers)
    }

    @Test
    fun `resource usage defaults to zero until a sample has landed`() {
        val service = local()
        assertEquals(0.0, ServiceProtoMapper.toProto(service).cpuUsage)
        assertEquals(0.0, ServiceProtoMapper.toProto(service).usedMemory)
    }

    @Test
    fun `mappers carry resource usage sampled by the ping loop`() {
        val service = local()
        service.cpuUsage = 42.5
        service.usedMemory = 512.0

        assertEquals(42.5, ServiceProtoMapper.toProto(service).cpuUsage)
        assertEquals(512.0, ServiceProtoMapper.toProto(service).usedMemory)

        val shared = ServiceEventMapper.toShared(service)
        assertEquals(42.5, shared.cpuUsage)
        assertEquals(512.0, shared.usedMemory)
    }

    @Test
    fun `mappers carry the motd set by the ping loop`() {
        val service = local()
        service.motd = "A Minecraft Server"

        assertEquals("A Minecraft Server", ServiceProtoMapper.toProto(service).motd)
        assertEquals("A Minecraft Server", ServiceProcessProtoMapper.toProto(service).motd)
        assertEquals("A Minecraft Server", ServiceEventMapper.toShared(service).motd)
    }

    @Test
    fun `motd defaults to empty until a ping has landed`() {
        val service = local()
        assertEquals("", ServiceProtoMapper.toProto(service).motd)
        assertEquals("", ServiceEventMapper.toShared(service).motd)
    }

    @Test
    fun `ServiceProtoMapper maps fields, host and properties`() {
        val service = local()
        val data = ServiceProtoMapper.toProto(service)

        assertEquals(service.id.toString(), data.id)
        assertEquals(1, data.index)
        assertEquals("lobby", data.group)
        assertEquals("RUNNING", data.state)
        assertEquals(30000, data.port)
        assertEquals("10.0.0.5", data.host)
        assertEquals(-1L, data.pid) // no process attached
        assertEquals("true", data.propertiesMap["fallback"])
    }

    @Test
    fun `ServiceEventMapper maps to the shared model with properties`() {
        val service = local(state = ServiceState.STARTING)
        val shared = ServiceEventMapper.toShared(service)

        assertEquals("lobby-1", shared.name())
        assertEquals(ServiceState.STARTING, shared.state)
        assertEquals("10.0.0.5", shared.host)
        assertEquals(-1L, shared.pid)
        assertTrue(shared.isFallback())
    }

    @Test
    fun `ServiceProcessProtoMapper maps fields, state name, node id and properties`() {
        val service = local(state = ServiceState.RUNNING, index = 3, group = "proxy", port = 25565)
        val proto = ServiceProcessProtoMapper.toProto(service, nodeId = "node-1")

        assertEquals(service.id.toString(), proto.uuid)
        assertEquals(3, proto.index)
        assertEquals("proxy", proto.plan)
        assertEquals("node-1", proto.nodeId)
        assertEquals(25565, proto.boundPort)
        assertEquals("RUNNING", proto.state)
        assertEquals("true", proto.propertiesMap["fallback"])
    }

    @Test
    fun `ServiceProcessProtoMapper carries every state name losslessly`() {
        ServiceState.entries.forEach { state ->
            assertEquals(state.name, ServiceProcessProtoMapper.toProto(local(state = state)).state)
        }
    }
}
