package de.polocloud.node.cluster.heartbeat

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.proto.NodeState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.seconds

/**
 * Covers [NodeHeartBeatMonitor.checkAll]'s liveness-reference fallback: a node with no
 * heartbeat row at all (scheduler never started, or died) must still be caught by the
 * same timeout via [NodeData.lastConnection], not exempted from crash detection forever.
 * Uses a real (throwaway, file-backed) H2 database like [de.polocloud.node.services.queue.ServiceQueueEligibilityTest],
 * since [NodeRepository]/[NodeHeartBeatRepository] talk to [DatabaseAccess] directly.
 */
class NodeHeartBeatMonitorTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-heartbeat-monitor-test-${UUID.randomUUID()}"

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

    private val timeout = 15.seconds
    private val monitor = NodeHeartBeatMonitor(NodeElectionService(), timeout)

    private fun onlineNode(lastConnection: kotlin.time.Instant, index: Int) =
        node(lastConnection, index, NodeState.ONLINE)

    private fun node(lastConnection: kotlin.time.Instant, index: Int, state: NodeState) = NodeData(
        id = UUID.randomUUID(),
        nodeIndex = index,
        groupName = "node",
        hostname = "10.0.0.$index",
        port = 4240 + index,
        state = state,
        head = false,
        electedAt = null,
        term = 0,
        votedFor = null,
        version = "3",
        gitCommitHash = "abc",
        firstConnection = now(),
        lastConnection = lastConnection,
        maxMemory = 0,
    ).also { NodeRepository.save(it) }

    @Test
    fun `a node with no heartbeat ever and a stale lastConnection is marked crashed`() {
        val node = onlineNode(lastConnection = now() - timeout - 5.seconds, index = 1)

        monitor.checkAll()

        assertEquals(NodeState.CRASHED, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `a node with no heartbeat yet but a recent lastConnection is not marked crashed`() {
        val node = onlineNode(lastConnection = now(), index = 2)

        monitor.checkAll()

        assertEquals(NodeState.ONLINE, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `a stale heartbeat left over from before a restart does not crash a freshly reconnected node`() {
        val node = onlineNode(lastConnection = now(), index = 3)
        // Simulates a leftover row from a previous run: older than lastConnection, and
        // on its own older than the crash threshold too.
        NodeHeartBeatRepository.save(
            NodeHeartBeat(
                id = UUID.randomUUID().toString(),
                nodeId = node.id,
                heartBeatAt = now() - timeout - 30.seconds,
                systemCpuUsage = 1.0,
                systemMemoryUsage = 1.0,
                applicationCpuUsage = 1.0,
                applicationMemoryUsage = 1.0,
                tps = 20.0,
            )
        )

        monitor.checkAll()

        assertEquals(NodeState.ONLINE, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `a genuinely stale heartbeat marks the node crashed`() {
        val node = onlineNode(lastConnection = now() - timeout - 30.seconds, index = 4)
        NodeHeartBeatRepository.save(
            NodeHeartBeat(
                id = UUID.randomUUID().toString(),
                nodeId = node.id,
                heartBeatAt = now() - timeout - 5.seconds,
                systemCpuUsage = 1.0,
                systemMemoryUsage = 1.0,
                applicationCpuUsage = 1.0,
                applicationMemoryUsage = 1.0,
                tps = 20.0,
            )
        )

        monitor.checkAll()

        assertEquals(NodeState.CRASHED, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `a fresh heartbeat keeps the node online`() {
        val node = onlineNode(lastConnection = now() - timeout - 30.seconds, index = 5)
        NodeHeartBeatRepository.save(
            NodeHeartBeat(
                id = UUID.randomUUID().toString(),
                nodeId = node.id,
                heartBeatAt = now(),
                systemCpuUsage = 1.0,
                systemMemoryUsage = 1.0,
                applicationCpuUsage = 1.0,
                applicationMemoryUsage = 1.0,
                tps = 20.0,
            )
        )

        monitor.checkAll()

        assertEquals(NodeState.ONLINE, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `a node stuck in STARTING with a stale lastConnection is marked crashed`() {
        // Not just ONLINE nodes: one that died before finishing startup must also reach
        // CRASHED eventually, otherwise it's never eligible for NodePruneService and
        // permanently inflates the election quorum denominator.
        val node = node(lastConnection = now() - timeout - 5.seconds, index = 6, state = NodeState.STARTING)

        monitor.checkAll()

        assertEquals(NodeState.CRASHED, NodeRepository.find(node.id)?.state)
    }

    @Test
    fun `an already stopped node is left untouched`() {
        val node = node(lastConnection = now() - timeout - 5.seconds, index = 7, state = NodeState.STOPPED)

        monitor.checkAll()

        assertEquals(NodeState.STOPPED, NodeRepository.find(node.id)?.state)
    }
}
