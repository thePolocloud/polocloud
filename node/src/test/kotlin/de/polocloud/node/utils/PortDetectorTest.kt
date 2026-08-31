package de.polocloud.node.utils

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.Service
import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Regression coverage for the port-selection race that let concurrently-starting
 * services collide on the same port: [PortDetector.nextPort] used to only consult
 * [de.polocloud.node.services.ServiceRepository] (a DB row that isn't written until deep
 * into `FactoryService.start`, well after the port is chosen) and a transient
 * bind-then-release [java.net.ServerSocket] check — neither of which sees a sibling
 * service that is mid-start on another thread. `nextPort` now also claims each picked
 * port in an in-memory reserved set until [PortDetector.release] is called, closing that
 * window.
 */
class PortDetectorTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-port-detector-test-${UUID.randomUUID()}"

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

    private val platform = Platform(name = "paper", type = "SERVER", language = "JAVA")

    private fun service(nodeId: String) =
        LocalService(Service(UUID.randomUUID(), 0, "lobby", ServiceState.QUEUED, "127.0.0.1", -1, nodeId))

    @RepeatedTest(5)
    fun `concurrent nextPort calls for the same node never pick the same port`() {
        val nodeId = "node-${UUID.randomUUID()}"
        val workers = 8
        val pool = Executors.newFixedThreadPool(workers)
        val ready = CountDownLatch(workers)
        val go = CountDownLatch(1)

        val futures = (1..workers).map {
            pool.submit<Int> {
                ready.countDown(); go.await()
                PortDetector.nextPort(service(nodeId), platform)
            }
        }

        var ports: List<Int> = emptyList()
        try {
            assertTimeoutPreemptively(10.seconds.toJavaDuration()) {
                ready.await()
                go.countDown()
                ports = futures.map { it.get(5, TimeUnit.SECONDS) }
            }
        } finally {
            pool.shutdownNow()
        }

        assertEquals(workers, ports.toSet().size, "expected $workers distinct ports, got $ports")

        ports.forEach { PortDetector.release(nodeId, it) }
    }

    @Test
    fun `release lets a freed port be picked again`() {
        val nodeId = "node-${UUID.randomUUID()}"
        val first = PortDetector.nextPort(service(nodeId), platform)
        PortDetector.release(nodeId, first)

        val second = PortDetector.nextPort(service(nodeId), platform)
        assertEquals(first, second)

        PortDetector.release(nodeId, second)
    }
}
