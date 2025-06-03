package dev.httpmarco.polocloud.sdk.java.groups

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

class GroupProviderTest {

    @Test
    @DisplayName("findGroups should return all groups")
    fun findGroups() {
        val groupProvider = GroupProvider()
        val groups = groupProvider.find()

        TODO()
    }

    @Test
    @DisplayName("findGroupByName should return a group by its name")
    fun findGroupByName() {
        val groupProvider = GroupProvider()
        val groups = groupProvider.find("test")
    }
}