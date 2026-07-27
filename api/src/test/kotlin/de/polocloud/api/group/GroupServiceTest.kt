package de.polocloud.api.group

import de.polocloud.proto.GroupData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * In-memory [GroupApiClient] so the API surface can be tested without a node.
 */
private class FakeGroupApiClient(initial: List<GroupData> = emptyList()) : GroupApiClient {

    val storage = initial.associateBy { it.name }.toMutableMap()
    val deleted = mutableListOf<String>()

    override suspend fun findGroups(nameFilter: String?, typeFilter: String?): List<GroupData> {
        return storage.values.filter { data ->
            (nameFilter == null || data.name.contains(nameFilter, ignoreCase = true)) &&
                (typeFilter == null || data.platform.equals(typeFilter, ignoreCase = true))
        }
    }

    override suspend fun createGroup(data: GroupData): GroupData = data.also { storage[it.name] = it }
    override suspend fun updateGroup(data: GroupData): GroupData = data.also { storage[it.name] = it }
    override suspend fun deleteGroup(name: String): Boolean {
        deleted += name
        return storage.remove(name) != null
    }
}

class GroupServiceTest {

    private fun data(name: String, platform: String) = GroupData.newBuilder()
        .setName(name)
        .setMemory(512)
        .setStartThreshold(0.5)
        .setMinOnline(1)
        .setMaxOnline(2)
        .setPlatform(platform)
        .setVersion("1.21")
        .addTemplates("GLOBAL")
        .addTemplates(name)
        .build()

    private fun service(vararg groups: GroupData) = GroupService(FakeGroupApiClient(groups.toList()))

    @Test
    fun `findAll maps all proto groups to api groups`() {
        val service = service(data("Lobby", "velocity"), data("Survival", "paper"))

        val all = service.findAll()

        assertEquals(2, all.size)
        assertTrue(all.any { it.name == "Lobby" && it.platform == "velocity" })
    }

    @Test
    fun `find by name returns the matching group`() {
        val service = service(data("Lobby", "velocity"), data("Survival", "paper"))

        assertEquals("Lobby", service.find("Lobby")?.name)
        assertNull(service.find("Unknown"))
    }

    @Test
    fun `find by type classifies proxy and server platforms`() {
        val service = service(
            data("Lobby", "velocity"),
            data("Proxy", "bungeecord"),
            data("Survival", "paper"),
        )

        assertEquals(setOf("Lobby", "Proxy"), service.find(GroupFilterType.PROXY).map { it.name }.toSet())
        assertEquals(setOf("Survival"), service.find(GroupFilterType.SERVER).map { it.name }.toSet())
    }

    @Test
    fun `count reflects the number of groups`() {
        val service = service(data("Lobby", "velocity"), data("Survival", "paper"))

        assertEquals(2, service.count())
    }

    @Test
    fun `create builds a group and persists it on submit`() {
        val client = FakeGroupApiClient()
        val service = GroupService(client)

        val created = service.create("Lobby")
            .memory(1024)
            .platform("velocity")
            .version("3.5.0")
            .submit()

        assertEquals("Lobby", created.name)
        assertEquals(1024, created.memory)
        assertTrue(client.storage.containsKey("Lobby"))
    }

    @Test
    fun `edit applies the editor and updates the group`() {
        val client = FakeGroupApiClient(listOf(data("Lobby", "velocity")))
        val service = GroupService(client)

        service.edit("Lobby") { builder ->
            builder.platform("paper").memory(2048)
        }

        assertEquals("paper", client.storage["Lobby"]!!.platform)
        assertEquals(2048, client.storage["Lobby"]!!.memory)
    }

    @Test
    fun `edit preserves fields the editor did not touch`() {
        val client = FakeGroupApiClient(listOf(data("Lobby", "velocity")))
        val service = GroupService(client)

        service.edit("Lobby") { builder -> builder.memory(2048) }

        val updated = client.storage["Lobby"]!!
        assertEquals(2048, updated.memory)
        assertEquals(0.5, updated.startThreshold)
        assertEquals(1, updated.minOnline)
        assertEquals(2, updated.maxOnline)
        assertEquals("velocity", updated.platform)
        assertEquals("1.21", updated.version)
        assertEquals(listOf("GLOBAL", "Lobby"), updated.templatesList)
    }

    @Test
    fun `edit can append templates without losing existing ones`() {
        val client = FakeGroupApiClient(listOf(data("Lobby", "velocity")))
        val service = GroupService(client)

        service.edit("Lobby") { builder -> builder.template("GLOBAL_SERVER") }

        assertEquals(listOf("GLOBAL", "Lobby", "GLOBAL_SERVER"), client.storage["Lobby"]!!.templatesList)
    }

    @Test
    fun `edit throws when no group with that name exists`() {
        val service = service(data("Lobby", "velocity"))

        assertThrows(NoSuchElementException::class.java) {
            service.edit("Unknown") { }
        }
    }

    @Test
    fun `delete by name removes the group`() {
        val client = FakeGroupApiClient(listOf(data("Lobby", "velocity")))
        val service = GroupService(client)

        service.delete("Lobby")

        assertEquals(listOf("Lobby"), client.deleted)
        assertTrue(client.storage.isEmpty())
    }
}
