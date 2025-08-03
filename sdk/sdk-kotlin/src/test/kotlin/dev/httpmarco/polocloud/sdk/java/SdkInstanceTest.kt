package dev.httpmarco.polocloud.sdk.java

import dev.httpmarco.polocloud.sdk.kotlin.Polocloud
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class SdkInstanceTest {

    @Test
    @DisplayName("Test Polocloud Instance Creation")
    fun testInstance() {
        // This test is to ensure that the Polocloud instance is created correctly.
        // The actual test logic can be expanded as needed.
        val polocloudInstance = Polocloud.instance()
        assertNotNull(polocloudInstance) { "Polocloud instance should not be null" }
    }

    @Test
    @DisplayName("Test Service Provider Retrieval")
    fun testServiceProvider() {
        // This test checks if the service provider can be retrieved from the Polocloud instance.
        val serviceProvider = Polocloud.instance().serviceProvider()
        assertNotNull(serviceProvider) { "Service provider should not be null" }
    }

    @Test
    @DisplayName("Test Group Provider Retrieval")
    fun testGroupProvider() {
        // This test checks if the group provider can be retrieved from the Polocloud instance.
        val groupProvider = Polocloud.instance().groupProvider()
        assertNotNull(groupProvider) { "Group provider should not be null" }
    }

    @Test
    @DisplayName("Test Plugin Provider Retrieval")
    fun testSdkResponse() {
        // This test checks if the plugin provider can be retrieved from the Polocloud instance.
        val polocloudInstance = Polocloud.instance()

        assert(polocloudInstance.available()) { "Plugin provider should be available" }
    }
}