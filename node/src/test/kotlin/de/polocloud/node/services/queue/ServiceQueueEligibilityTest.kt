package de.polocloud.node.services.queue

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.group.Group
import de.polocloud.node.group.TemplateCodec
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoServiceProcessData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Covers [ServiceQueue]'s node-eligibility and cluster-wide `minOnline` placement math.
 * [ServiceQueue.groups] and [ServiceQueue.onlineNodes] are injected directly
 * (mirroring how `ListServicesServerHandler`/`FindServicesServerHandler` inject their
 * `peers` supplier for the same reason), but queuing a service still persists the
 * placeholder via [ServiceProvider.update] like it does in production, so this needs a
 * real (throwaway, file-backed) H2 database rather than a fake repository.
 */
class ServiceQueueEligibilityTest {

    companion object {
        // Relative: DatabaseCredentials.H2 always builds its JDBC URL as "jdbc:h2:file:./<path>",
        // so an absolute (e.g. Windows drive-letter) path here would produce an invalid URL.
        private val dbPath = "build/tmp/polocloud-service-queue-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUpDatabase() {
            // The connection/close paths log through the i18n helpers, which require the
            // TranslationService to be initialised once before any database access.
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

    // Lexicographically ordered so sorting by id.toString() is predictable in tests.
    private val selfId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val peerAId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val peerBId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    // maxMemory defaults to 0 ("unknown/unlimited" — see NodeData.maxMemory) so existing
    // tests aren't affected by the memory-capacity cap; the capacity tests below opt in.
    private fun node(id: UUID, name: String, maxMemory: Int = 0) =
        NodeData(id = id, index = 1, groupName = name, hostname = "10.0.0.1", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc", maxMemory = maxMemory)

    private fun group(name: String = "lobby", minOnline: Long = 1, nodes: List<String> = emptyList()) =
        Group(name, 512, 0.0, minOnline, 10, "PAPER", "1.21", nodesJson = TemplateCodec.encode(nodes))

    private fun queue(
        provider: ServiceProvider = ServiceProvider(nodeId = selfId.toString()),
        online: List<NodeData>,
        groups: List<Group>,
        peerQuery: PeerServiceQuery = PeerServiceQuery { _, _ -> emptyList() },
        // Defaults to "every node equally idle" so existing tests only need to reason
        // about running counts; load-aware placement gets its own dedicated test below.
        loadProvider: NodeLoadProvider = NodeLoadProvider { 0.0 },
    ) = ServiceQueue(
        factory = FactoryService(PlatformService(), provider),
        serviceProvider = provider,
        groups = { groups },
        onlineNodes = { online },
        peerQuery = peerQuery,
        loadProvider = loadProvider,
    )

    private fun peerService(groupName: String, index: Int) = ProtoServiceProcessData.newBuilder()
        .setUuid(UUID.randomUUID().toString()).setPlan(groupName).setIndex(index).setState("RUNNING").build()

    @Test
    fun `unrestricted group with nothing running assigns exactly one replica to the owning node`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val peerB = node(peerBId, "node-c")
        val g = group(minOnline = 3)

        val q = queue(online = listOf(self, peerA, peerB), groups = listOf(g))
        q.enqueueRequiredForTest()

        // self sorts first (selfId < peerAId < peerBId) -> position 0 owns replica k=0.
        assertEquals(listOf(1), q.queuedIndexes("lobby"))
    }

    @Test
    fun `the 2 missing replicas go to the 2 nodes not already running one`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val peerB = node(peerBId, "node-c")
        val g = group(minOnline = 3)

        // peerA already runs one instance; with all nodes equally (un)loaded, placement
        // ties break on running-count-for-this-group first, so the 2 still-needed
        // replicas go to self and peerB (both at 0), not to peerA (already at 1).
        val q = queue(
            online = listOf(self, peerA, peerB),
            groups = listOf(g),
            peerQuery = PeerServiceQuery { peer, _ -> if (peer.id == peerAId) listOf(peerService("lobby", 1)) else emptyList() },
        )
        q.enqueueRequiredForTest()

        // Index 1 is already taken by peerA's existing instance, so self's replica lands on 2.
        assertEquals(listOf(2), q.queuedIndexes("lobby"))
    }

    @Test
    fun `a node excluded from the group's node whitelist never enqueues`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val g = group(minOnline = 5, nodes = listOf(peerA.name()))

        val q = queue(online = listOf(self, peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a group whitelisted to just this node claims its whole minOnline locally`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val g = group(minOnline = 2, nodes = listOf(self.name()))

        val q = queue(online = listOf(self, peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertEquals(listOf(1, 2), q.queuedIndexes("lobby").sorted())
    }

    @Test
    fun `no eligible online node leaves the group untouched`() {
        val peerA = node(peerAId, "node-b")
        // self is not online, and the group is not restricted to peerA either - but peerA
        // is the only online node and self isn't part of the eligible set at all.
        val g = group(minOnline = 3, nodes = listOf("node-does-not-exist"))

        val q = queue(online = listOf(peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a heavily loaded node is skipped in favor of an idle one, even with equal running counts`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val g = group(minOnline = 1)

        // Neither node runs an instance of this group yet, so without load data this
        // would tie-break to self (lower id). A 90%-loaded self should lose that tie to
        // an idle peerA instead.
        val q = queue(
            online = listOf(self, peerA),
            groups = listOf(g),
            loadProvider = NodeLoadProvider { node -> if (node.id == selfId) 90.0 else 5.0 },
        )
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a node at capacity is skipped in favor of one with room`() {
        val self = node(selfId, "node-a", maxMemory = 400) // group needs 512MB — no room
        val peerA = node(peerAId, "node-b", maxMemory = 1000)
        val g = group(minOnline = 1)

        val selfQueue = queue(online = listOf(self, peerA), groups = listOf(g))
        selfQueue.enqueueRequiredForTest()
        assertTrue(selfQueue.queuedIndexes("lobby").isEmpty())

        // Confirm it's not simply lost: peerA's own perspective claims the replica.
        val peerAsSelfQueue = queue(
            provider = ServiceProvider(nodeId = peerAId.toString()),
            online = listOf(self, peerA),
            groups = listOf(g),
        )
        peerAsSelfQueue.enqueueRequiredForTest()
        assertEquals(listOf(1), peerAsSelfQueue.queuedIndexes("lobby"))
    }

    @Test
    fun `no eligible node has room, so the replica is left unassigned rather than overbooking anyone`() {
        val self = node(selfId, "node-a", maxMemory = 100)
        val peerA = node(peerAId, "node-b", maxMemory = 100)
        val g = group(minOnline = 1)

        val q = queue(online = listOf(self, peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a group backing off from repeated crashes is not placed even though it needs replicas`() {
        val self = node(selfId, "node-a")
        val g = group(minOnline = 1)
        val provider = ServiceProvider(nodeId = selfId.toString())

        // Three consecutive fast failures trip CrashLoopGuard's backoff window.
        provider.crashLoopGuard.recordExit(g.name, ranForMillis = 100)
        provider.crashLoopGuard.recordExit(g.name, ranForMillis = 100)
        provider.crashLoopGuard.recordExit(g.name, ranForMillis = 100)

        val q = queue(provider = provider, online = listOf(self), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `removeGroup drops a queued service from both the in-memory queue and the database`() {
        val self = node(selfId, "node-a")
        val g = group(minOnline = 1)
        val provider = ServiceProvider(nodeId = selfId.toString())

        val q = queue(provider = provider, online = listOf(self), groups = listOf(g))
        q.enqueueRequiredForTest()
        assertEquals(listOf(1), q.queuedIndexes("lobby"))

        q.removeGroup("lobby")

        assertTrue(q.queuedIndexes("lobby").isEmpty())
        assertTrue(provider.findAll().none { it.groupName.equals("lobby", ignoreCase = true) })
    }

    @Test
    fun `removeGroup is case-insensitive and leaves other groups' queued services untouched`() {
        val self = node(selfId, "node-a")
        val lobby = group(name = "lobby", minOnline = 1)
        val survival = group(name = "survival", minOnline = 1)
        val provider = ServiceProvider(nodeId = selfId.toString())

        val q = queue(provider = provider, online = listOf(self), groups = listOf(lobby, survival))
        q.enqueueRequiredForTest()
        assertEquals(listOf(1), q.queuedIndexes("lobby"))
        assertEquals(listOf(1), q.queuedIndexes("survival"))

        q.removeGroup("LOBBY")

        assertTrue(q.queuedIndexes("lobby").isEmpty())
        assertEquals(listOf(1), q.queuedIndexes("survival"))
    }

    @Test
    fun `next index avoids an index already used by a peer`() {
        // peerAId sorts before selfId here so self ends up at position 1, owning k=0.
        val lowId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val self = node(selfId, "node-a")
        val peer = node(lowId, "node-z")
        val g = group(minOnline = 2)

        val q = queue(
            online = listOf(self, peer),
            groups = listOf(g),
            peerQuery = PeerServiceQuery { _, _ -> listOf(peerService("lobby", 1)) },
        )
        q.enqueueRequiredForTest()

        assertEquals(listOf(2), q.queuedIndexes("lobby"))
    }
}
