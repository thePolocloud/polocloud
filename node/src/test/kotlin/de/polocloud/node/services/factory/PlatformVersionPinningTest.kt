package de.polocloud.node.services.factory

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.group.Group
import de.polocloud.node.services.factory.platform.PlatformVersion
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Regression coverage for the "bad platform update" bug: PoloCloud used to have no memory
 * of which build of a pinned Minecraft version (e.g. "1.21.4") previously worked, so a group
 * would keep re-resolving to a freshly-published build that crash-loops forever, even though
 * the last-known-good jar was still sitting untouched in PlatformJarCache. See
 * PlatformVersionPinning.
 */
class PlatformVersionPinningTest {

    companion object {
        private val dbPath = "build/tmp/polocloud-version-pinning-test-${UUID.randomUUID()}"

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

    private fun group(name: String, version: String = "1.21.4") =
        Group(name, 512, 0.8, 1, 1, "paper", version)

    private fun version(build: Int, mcVersion: String = "1.21.4") =
        PlatformVersion(version = mcVersion, build = build, downloadUrl = "https://example.invalid/$build.jar")

    @Test
    fun `resolve returns the given version unchanged when no pin exists yet`() {
        val group = group("lobby-${UUID.randomUUID()}")
        val resolved = version(42)

        assertSame(resolved, PlatformVersionPinning.resolve(group, resolved))
    }

    @Test
    fun `falls back to the last known-good build once the newly-resolved one is quarantined`() {
        val group = group("lobby-${UUID.randomUUID()}")

        PlatformVersionPinning.recordSuccess(group, build = 10)
        PlatformVersionPinning.recordFailure(group, build = 11)

        val fallback = PlatformVersionPinning.resolve(group, version(11))

        assertEquals(10, fallback.build)
        assertEquals("", fallback.downloadUrl)
        assertEquals(group.version, fallback.version)
    }

    @Test
    fun `does not fall back for a build that was never quarantined`() {
        val group = group("lobby-${UUID.randomUUID()}")
        PlatformVersionPinning.recordSuccess(group, build = 10)
        PlatformVersionPinning.recordFailure(group, build = 11)

        // A third, different build (e.g. the operator manually bumped again) is untouched.
        val resolved = version(12)
        assertSame(resolved, PlatformVersionPinning.resolve(group, resolved))
    }

    @Test
    fun `a pin recorded for a different Minecraft version string does not apply`() {
        val groupName = "lobby-${UUID.randomUUID()}"
        val originalGroup = group(groupName, version = "1.21.4")
        PlatformVersionPinning.recordSuccess(originalGroup, build = 10)
        PlatformVersionPinning.recordFailure(originalGroup, build = 11)

        // Operator repoints the same group at a different Minecraft version afterwards —
        // the earlier good/bad build numbers were for "1.21.4" and are meaningless here,
        // even though the build number (11) happens to collide.
        val bumpedGroup = group(groupName, version = "1.22.0")
        val resolvedForNewVersion = version(11, mcVersion = "1.22.0")
        assertSame(resolvedForNewVersion, PlatformVersionPinning.resolve(bumpedGroup, resolvedForNewVersion))
    }

    @Test
    fun `recordFailure never quarantines a build already known to work`() {
        val group = group("lobby-${UUID.randomUUID()}")
        PlatformVersionPinning.recordSuccess(group, build = 10)
        // A flaky one-off crash of the already-proven build must not quarantine it.
        PlatformVersionPinning.recordFailure(group, build = 10)

        val resolved = version(10)
        assertSame(resolved, PlatformVersionPinning.resolve(group, resolved))
    }

    @Test
    fun `recordSuccess clears a quarantine once that build comes online after all`() {
        val group = group("lobby-${UUID.randomUUID()}")
        PlatformVersionPinning.recordSuccess(group, build = 10)
        PlatformVersionPinning.recordFailure(group, build = 11)
        // build 11 turns out to just have been a flaky one-off failure.
        PlatformVersionPinning.recordSuccess(group, build = 11)

        val resolved = version(11)
        assertSame(resolved, PlatformVersionPinning.resolve(group, resolved))
    }
}
