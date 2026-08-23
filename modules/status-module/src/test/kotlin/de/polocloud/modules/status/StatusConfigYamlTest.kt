package de.polocloud.modules.status

import de.polocloud.moduleapi.config.ModuleConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * [ModuleConfig] is plain snakeyaml JavaBean (de)serialization with no generics
 * registered — this pins down that a nested `Map<String, StatusGroupConfig>` survives
 * a save/reload round-trip (and a fully hand-written file) as actual [StatusGroupConfig]
 * instances, not raw maps, since nothing else in the codebase exercises that path yet.
 */
class StatusConfigYamlTest {

    @Test
    fun `default config round-trips through save and reload with real StatusGroupConfig instances`() {
        val dir = Files.createTempDirectory("status-config-test").toFile()
        val file = dir.resolve("config.yml")

        val config = ModuleConfig(file, StatusConfig::class.java) { StatusConfig() }
        assertTrue(file.isFile)

        val reopened = ModuleConfig(file, StatusConfig::class.java) { StatusConfig() }
        val group = reopened.value.groups.getValue("bedwars")
        assertEquals("BedWars", group.displayName)
        assertTrue(group.available)
    }

    @Test
    fun `a hand-written groups map is parsed into StatusGroupConfig`() {
        val dir = Files.createTempDirectory("status-config-test").toFile()
        val file = dir.resolve("config.yml")
        file.writeText(
            """
            host: 0.0.0.0
            port: 7020
            hostWebsite: true
            corsEnabled: true
            groups:
              bedwars:
                displayName: BedWars
                available: false
              skywars:
                displayName: SkyWars
                available: true
            """.trimIndent()
        )

        val config = ModuleConfig(file, StatusConfig::class.java) { StatusConfig() }

        val bedwars = config.value.groups.getValue("bedwars")
        assertEquals("BedWars", bedwars.displayName)
        assertFalse(bedwars.available)

        val skywars = config.value.groups.getValue("skywars")
        assertEquals("SkyWars", skywars.displayName)
        assertTrue(skywars.available)
    }
}
