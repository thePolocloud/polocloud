package de.polocloud.node.communication.handler.group

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.CreateGroupRequest
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.GroupData
import de.polocloud.proto.GroupListRequest
import de.polocloud.proto.UpdateGroupRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * In-memory [GroupService] used as a test seam so the handlers can be exercised
 * without a database.
 */
private class InMemoryGroupService(initial: List<Group> = emptyList()) : GroupService() {

    val storage = initial.associateBy { it.name }.toMutableMap()

    override fun findAll(): List<Group> = storage.values.toList()
    override fun exists(name: String): Boolean = storage.containsKey(name)
    override fun find(name: String): Group? = storage[name]
    override fun create(group: Group): Group = group.also { storage[it.name] = it }
    override fun update(group: Group): Group = group.also { storage[it.name] = it }
    override fun delete(group: Group) { storage.remove(group.name) }
}

class GroupServerHandlerTest {

    private val lobby = Group("Lobby", 512, 0.8, 1, 3, "velocity", "3.5.0")
    private val proxy = Group("Proxy", 256, 0.5, 1, 2, "velocity", "3.5.0")
    private val survival = Group("Survival", 1024, 0.9, 0, 5, "paper", "1.21")

    private fun service() = InMemoryGroupService(listOf(lobby, proxy, survival))

    private fun groupData(name: String, platform: String = "paper") = GroupData.newBuilder()
        .setName(name)
        .setMemory(512)
        .setStartThreshold(0.5)
        .setMinOnline(1)
        .setMaxOnline(2)
        .setPlatform(platform)
        .setVersion("1.21")
        .build()

    @Test
    fun `find returns all groups when no filter is given`() = runBlocking {
        val handler = GetGroupInformationServerHandler(service())

        val response = handler.handle(GroupListRequest.getDefaultInstance(), GrpcServerContext())

        assertEquals(3, response.groupsCount)
    }

    @Test
    fun `find filters by name case-insensitively`() = runBlocking {
        val handler = GetGroupInformationServerHandler(service())

        val response = handler.handle(
            GroupListRequest.newBuilder().setNameFilter("lob").build(),
            GrpcServerContext()
        )

        assertEquals(1, response.groupsCount)
        assertEquals("Lobby", response.groupsList.single().name)
    }

    @Test
    fun `find filters by platform via type filter`() = runBlocking {
        val handler = GetGroupInformationServerHandler(service())

        val response = handler.handle(
            GroupListRequest.newBuilder().setTypeFilter("VELOCITY").build(),
            GrpcServerContext()
        )

        assertEquals(setOf("Lobby", "Proxy"), response.groupsList.map { it.name }.toSet())
    }

    @Test
    fun `create persists a new group and returns it`() = runBlocking {
        val service = InMemoryGroupService()
        val handler = CreateGroupServerHandler(service)

        val response = handler.handle(
            CreateGroupRequest.newBuilder().setGroup(groupData("New")).build(),
            GrpcServerContext()
        )

        assertEquals("New", response.name)
        assertTrue(service.exists("New"))
    }

    @Test
    fun `create rejects a duplicate group`() {
        val handler = CreateGroupServerHandler(service())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    CreateGroupRequest.newBuilder().setGroup(groupData("Lobby")).build(),
                    GrpcServerContext()
                )
            }
        }
    }

    @Test
    fun `update rejects an unknown group`() {
        val handler = UpdateGroupServerHandler(service())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    UpdateGroupRequest.newBuilder().setGroup(groupData("Ghost")).build(),
                    GrpcServerContext()
                )
            }
        }
    }

    @Test
    fun `update stores the new values for an existing group`() = runBlocking {
        val service = service()
        val handler = UpdateGroupServerHandler(service)

        val response = handler.handle(
            UpdateGroupRequest.newBuilder().setGroup(groupData("Lobby", platform = "paper")).build(),
            GrpcServerContext()
        )

        assertEquals("paper", response.platform)
        assertEquals("paper", service.find("Lobby")!!.platform)
    }

    @Test
    fun `delete removes an existing group and reports success`() = runBlocking {
        val service = service()
        val handler = DeleteGroupServerHandler(service, ServiceProvider())

        val response = handler.handle(
            DeleteGroupRequest.newBuilder().setName("Lobby").build(),
            GrpcServerContext()
        )

        assertTrue(response.success)
        assertFalse(service.exists("Lobby"))
    }

    @Test
    fun `delete reports failure for an unknown group`() = runBlocking {
        val handler = DeleteGroupServerHandler(service(), ServiceProvider())

        val response = handler.handle(
            DeleteGroupRequest.newBuilder().setName("Ghost").build(),
            GrpcServerContext()
        )

        assertFalse(response.success)
    }

    // Group mutation is CLI/peer-only — never exposed to a launched service via the SDK
    // (see CallerAuthorization). A "serviceSubject" in the context means the call arrived
    // via ServiceGrpcEndpoint, i.e. from a plugin, not a trusted CLI operator or peer node.
    private fun serviceCallContext() = GrpcServerContext().with("serviceSubject", "lobby-1")

    @Test
    fun `create rejects a call from a launched service`() {
        val handler = CreateGroupServerHandler(InMemoryGroupService())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    CreateGroupRequest.newBuilder().setGroup(groupData("New")).build(),
                    serviceCallContext()
                )
            }
        }
    }

    @Test
    fun `update rejects a call from a launched service`() {
        val handler = UpdateGroupServerHandler(service())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    UpdateGroupRequest.newBuilder().setGroup(groupData("Lobby", platform = "paper")).build(),
                    serviceCallContext()
                )
            }
        }
    }

    @Test
    fun `delete rejects a call from a launched service`() {
        val handler = DeleteGroupServerHandler(service(), ServiceProvider())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                handler.handle(
                    DeleteGroupRequest.newBuilder().setName("Lobby").build(),
                    serviceCallContext()
                )
            }
        }
    }
}
