package de.polocloud.node.player

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * CRUD round trip against a throwaway H2 database — mirrors
 * `node/src/test/kotlin/de/polocloud/node/services/queue/ServiceQueueEligibilityTest.kt`'s
 * lightweight per-class setup. No [de.polocloud.node.security.NodeCertificateStorage]
 * involved, so no singleton-contention concern with other test classes.
 */
class CloudPlayerRepositoryTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-player-repository-test-${UUID.randomUUID()}"

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

    private fun player(name: String = "Notch", currentServer: String? = "lobby-1") = CloudPlayer(
        id = UUID.randomUUID(),
        name = name,
        skinValue = "texture-value",
        skinSignature = "texture-signature",
        propertiesJson = "{}",
        currentProxy = "proxy-1",
        currentServer = currentServer,
    )

    @Test
    fun `save persists a player that findById can then find`() {
        val player = player()
        CloudPlayerRepository.save(player)

        val found = CloudPlayerRepository.findById(player.id)

        assertEquals(player.id, found?.id)
        assertEquals(player.name, found?.name)
        assertEquals(player.currentServer, found?.currentServer)
    }

    @Test
    fun `findByName matches case-insensitively`() {
        CloudPlayerRepository.save(player(name = "CaseTest"))

        assertEquals("CaseTest", CloudPlayerRepository.findByName("casetest")?.name)
        assertEquals("CaseTest", CloudPlayerRepository.findByName("CASETEST")?.name)
    }

    @Test
    fun `findByName returns null for an unknown player`() {
        assertNull(CloudPlayerRepository.findByName("does-not-exist-${UUID.randomUUID()}"))
    }

    @Test
    fun `delete removes the player`() {
        val player = player()
        CloudPlayerRepository.save(player)
        assertTrue(CloudPlayerRepository.findAll().any { it.id == player.id })

        CloudPlayerRepository.delete(player)

        assertNull(CloudPlayerRepository.findById(player.id))
    }

    @Test
    fun `save with a nullable currentServer round-trips as null`() {
        val player = player(currentServer = null)
        CloudPlayerRepository.save(player)

        assertNull(CloudPlayerRepository.findById(player.id)?.currentServer)
    }
}
