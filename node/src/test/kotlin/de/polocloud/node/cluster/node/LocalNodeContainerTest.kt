package de.polocloud.node.cluster.node

import de.polocloud.common.Address
import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.proto.NodeState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.time.Clock.System.now

/**
 * Covers [LocalNodeContainer.adoptJoinedIdentity] — the in-place identity adoption a live
 * `cluster join` (see [de.polocloud.node.identity.NodeIdentityService.joinLive]) uses
 * instead of replacing the container/[NodeData] object outright, so other components
 * already holding a reference to either never see it go stale.
 */
class LocalNodeContainerTest {

    companion object {
        // Relative: DatabaseCredentials.H2 always builds its JDBC URL as "jdbc:h2:file:./<path>".
        private val dbPath = "build/tmp/polocloud-local-node-container-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUpDatabase() {
            runCatching { TranslationService.init() }
            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }
        }

        @JvmStatic
        @AfterAll
        fun tearDownDatabase() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
        }
    }

    private fun node(id: UUID = UUID.randomUUID(), index: Int = 1, head: Boolean = false) =
        NodeFactory.create(
            id = id,
            index = index,
            groupName = "node",
            address = Address("127.0.0.1", 4239),
            version = "1.0.0",
            gitCommitHash = "abc123",
            head = head,
        )

    @Test
    fun `adoptJoinedIdentity copies the cluster-assigned fields onto the existing data object and persists them`() {
        val original = node(index = 1)
        NodeRepository.save(original)
        val container = LocalNodeContainer(original)

        val newFirstConnection = now()
        val fromCluster = original.copy(
            nodeIndex = 3,
            state = NodeState.ONLINE,
            head = true,
            term = 5,
            firstConnection = newFirstConnection,
        )

        container.adoptJoinedIdentity(fromCluster)

        // Mutated in place — `container.data` must stay the same object other components
        // may already hold a reference to, not be replaced.
        assert(container.data === original)
        assertEquals(3, container.data.nodeIndex)
        assertEquals(NodeState.ONLINE, container.data.state)
        assertEquals(true, container.data.head)
        assertEquals(5L, container.data.term)
        assertEquals(newFirstConnection, container.data.firstConnection)

        val persisted = NodeRepository.find(original.id)
        assertEquals(3, persisted?.nodeIndex)
        assertEquals(true, persisted?.head)
    }

    @Test
    fun `adoptJoinedIdentity rejects a NodeData describing a different node`() {
        val container = LocalNodeContainer(node())

        assertThrows(IllegalArgumentException::class.java) {
            container.adoptJoinedIdentity(node())
        }
    }
}
