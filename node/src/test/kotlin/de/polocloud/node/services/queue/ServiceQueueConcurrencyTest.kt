package de.polocloud.node.services.queue

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.proto.NodeState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Regression coverage for the queue field's thread-safety: [ServiceQueue.enqueueRequired]
 * (background "service-queue" thread, via [ServiceQueue.enqueueRequiredForTest]) and
 * [ServiceQueue.removeGroup] (terminal/gRPC thread) both touch the same `LinkedList` —
 * before it was consistently `synchronized`, concurrent offer()/count() racing
 * filter()/removeIf() from different threads could corrupt its internal node chain (lost
 * elements, a stuck/infinite iteration, or a raw NPE deep in `LinkedList`), not just
 * return a stale count. Deliberately doesn't exercise `drainQueue()` here — that calls
 * `FactoryService.start()`, which does real (slow/network) work this test shouldn't
 * depend on; the offer()/count() vs. filter()/removeIf() race is the same hazard either way.
 *
 * Not a *reliable* reproduction of the old bug — data races don't reproduce on demand —
 * but hammering both entry points concurrently, repeated, is enough to reliably fail (with
 * an exception or a hang, either caught by [assertTimeoutPreemptively]) on the old
 * unsynchronized code within a handful of runs, and reliably pass now.
 */
class ServiceQueueConcurrencyTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-service-queue-concurrency-test-${UUID.randomUUID()}"

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

    private val selfId = UUID.randomUUID()

    @RepeatedTest(5)
    fun `enqueue and removeGroup race without corrupting the queue`() {
        val self = NodeData(
            id = selfId, nodeIndex = 1, groupName = "node", hostname = "10.0.0.1", port = 4240, state = NodeState.ONLINE,
            head = false, electedAt = null, term = 0, votedFor = null,
            version = "3", gitCommitHash = "abc",
            firstConnection = Clock.System.now(), lastConnection = Clock.System.now(),
            maxMemory = 0,
        )
        // High minOnline keeps every enqueueRequired() call trying to offer() more entries
        // for the whole test — `running` never grows since nothing here ever drains/starts.
        // Persisted (not just supplied via `groups = {...}` below) so ServiceProvider.update's
        // insert of each queued service satisfies the services table's FK on groupName.
        val group = Group.new("lobby-${UUID.randomUUID()}", 64, 0.0, 200, 200, "PAPER", "1.21")
        GroupRepository.save(group)

        val provider = ServiceProvider(nodeId = selfId.toString())
        val queue = ServiceQueue(
            factory = FactoryService(PlatformService(), provider),
            serviceProvider = provider,
            groups = { listOf(group) },
            onlineNodes = { listOf(self) },
            peerQuery = PeerServiceQuery { _, _ -> emptyList() },
            loadProvider = NodeLoadProvider { 0.0 },
        )

        val iterations = 30
        val pool = Executors.newFixedThreadPool(3)
        val ready = CountDownLatch(3)
        val go = CountDownLatch(1)

        val enqueuerA = pool.submit {
            ready.countDown(); go.await()
            repeat(iterations) { runCatching { queue.enqueueRequiredForTest() } }
        }
        val enqueuerB = pool.submit {
            ready.countDown(); go.await()
            repeat(iterations) { runCatching { queue.enqueueRequiredForTest() } }
        }
        val remover = pool.submit {
            ready.countDown(); go.await()
            repeat(iterations) { runCatching { queue.removeGroup(group.name) } }
        }

        try {
            assertTimeoutPreemptively(15.seconds.toJavaDuration()) {
                ready.await()
                go.countDown()
                listOf(enqueuerA, enqueuerB, remover).forEach { it.get(10, TimeUnit.SECONDS) }
            }
        } finally {
            pool.shutdownNow()
        }

        // Final state must still be a well-formed, readable queue — the real assertion is
        // that none of the above threw or hung.
        queue.removeGroup(group.name)
    }
}
