package dev.httpmarco.polocloud.sdk.java.groups

import dev.httpmarco.polocloud.sdk.kotlin.Polocloud
import org.junit.jupiter.api.DisplayName
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.test.Test

class GroupProviderTest {

    @Test
    @DisplayName("findGroups should return all groups")
    fun findGroups() {
        val result = Polocloud.instance().groupProvider().find()

        assert(result.isNotEmpty())

        println("Found groups: ${result.joinToString(", ") { it.name() }}")
    }

    @Test
    @DisplayName("findGroups should return all groups bz async loading")
    fun findGroupsAsync() {
        val result = Polocloud.instance().groupProvider().findAsync()

        val groups = result.join()
        assert(groups.isNotEmpty())
        println("Found groups: ${groups.joinToString(", ") { it.name() }}")
    }

    @Test
    @DisplayName("findGroupByName should return a group by its name")
    fun findGroupByName() {
        val result = Polocloud.instance().groupProvider().find("lobby")

        assert(result != null)

        println("Found group: ${result!!.name()}")

    }

    @Test
    @DisplayName("findGroupByName should return a group by its name by async loading")
    fun findGroupByNameAsync() {
        val result = Polocloud.instance().groupProvider().findAsync("lobby")

        val group = result.join()
        assert(group != null)
        println("Found group: ${group!!.name()}")
    }


}