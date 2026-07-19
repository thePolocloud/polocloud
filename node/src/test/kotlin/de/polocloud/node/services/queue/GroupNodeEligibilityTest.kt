package de.polocloud.node.services.queue

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.group.Group
import de.polocloud.node.group.TemplateCodec
import de.polocloud.proto.NodeState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Covers [GroupNodeEligibility] directly, independent of [ServiceQueue]'s placement math
 * (see [ServiceQueueEligibilityTest] for that). Pure function, no database needed — the
 * online-node list is always passed in explicitly.
 */
class GroupNodeEligibilityTest {

    private fun node(name: String) =
        NodeData(id = UUID.randomUUID(), index = 1, groupName = name, hostname = "10.0.0.1", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc")

    private fun group(nodes: List<String> = emptyList()) =
        Group("lobby", 512, 0.0, 1, 10, "PAPER", "1.21", nodesJson = TemplateCodec.encode(nodes))

    @Test
    fun `an empty whitelist is unrestricted and every online node is eligible`() {
        val a = node("node-a")
        val b = node("node-b")

        val eligible = GroupNodeEligibility.eligibleOnlineNodes(group(nodes = emptyList()), listOf(a, b))

        assertEquals(listOf(a, b), eligible)
    }

    @Test
    fun `a non-empty whitelist restricts eligibility to matching node names`() {
        val a = node("node-a")
        val b = node("node-b")

        val eligible = GroupNodeEligibility.eligibleOnlineNodes(group(nodes = listOf(b.name())), listOf(a, b))

        assertEquals(listOf(b), eligible)
    }

    @Test
    fun `whitelist matching is case-sensitive on node name`() {
        val a = node("node-a")

        val eligible = GroupNodeEligibility.eligibleOnlineNodes(group(nodes = listOf(a.name().uppercase())), listOf(a))

        assertTrue(eligible.isEmpty())
    }

    @Test
    fun `a whitelisted node name that is not online yields no eligible nodes`() {
        val a = node("node-a")

        val eligible = GroupNodeEligibility.eligibleOnlineNodes(group(nodes = listOf("node-does-not-exist")), listOf(a))

        assertTrue(eligible.isEmpty())
    }

    @Test
    fun `no online nodes at all yields no eligible nodes regardless of whitelist`() {
        val eligible = GroupNodeEligibility.eligibleOnlineNodes(group(nodes = emptyList()), emptyList())

        assertTrue(eligible.isEmpty())
    }
}
