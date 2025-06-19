package dev.httpmarco.polocloud

import dev.httpmarco.polocloud.platforms.PlatformPool
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlatformTest {

    val url = "https://raw.githubusercontent.com/HttpMarco/polocloud/refs/heads/master/metadata/metadata.json";

    @Test
    @DisplayName("Load the table of platforms")
    fun testPlatformTable() {
        assert(PlatformPool.load(url).platforms.isNotEmpty())
    }

    @Test
    @DisplayName("Load the specific platform")
    fun testSpecificPlatform() {
        val platforms = PlatformPool.load(url).platforms

        platforms.forEach {
            println("Load platform: " + it.name)

            assertNotNull(it.name)
            assertNotNull(it.type)
            assertNotNull(it.language)
        }
    }

    @Test
    @DisplayName("Test the url mapping")
    fun urlMappingTest() {
        val platforms = PlatformPool.load(url).platforms

        platforms.forEach {
            assert(it.url.startsWith("https://"))
        }
    }
}