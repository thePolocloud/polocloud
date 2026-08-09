package de.polocloud.moduleapi.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ModuleConfigTest {

    data class SampleConfig(
        var apiToken: String = "default-token",
        var zoneId: String = "default-zone",
        var retries: Int = 3,
    )

    @Test
    fun `first access writes the default value to disk`() {
        val dir = Files.createTempDirectory("module-config-test").toFile()
        val file = dir.resolve("config.yml")

        val config = ModuleConfig(file, SampleConfig::class.java) { SampleConfig() }

        assertTrue(file.isFile)
        assertEquals("default-token", config.value.apiToken)
    }

    @Test
    fun `save persists changes and reload reads them back`() {
        val dir = Files.createTempDirectory("module-config-test").toFile()
        val file = dir.resolve("config.yml")

        val config = ModuleConfig(file, SampleConfig::class.java) { SampleConfig() }
        config.save(SampleConfig(apiToken = "real-token", zoneId = "zone-42", retries = 5))

        val reopened = ModuleConfig(file, SampleConfig::class.java) { SampleConfig() }
        assertEquals("real-token", reopened.value.apiToken)
        assertEquals("zone-42", reopened.value.zoneId)
        assertEquals(5, reopened.value.retries)
    }

    @Test
    fun `reload picks up a hand-edit made directly to the file`() {
        val dir = Files.createTempDirectory("module-config-test").toFile()
        val file = dir.resolve("config.yml")

        val config = ModuleConfig(file, SampleConfig::class.java) { SampleConfig() }
        assertEquals("default-token", config.value.apiToken)

        file.writeText("apiToken: hand-edited\nzoneId: default-zone\nretries: 3\n")

        val reloaded = config.reload()
        assertEquals("hand-edited", reloaded.apiToken)
        assertEquals("hand-edited", config.value.apiToken)
    }
}
