package de.polocloud.node.module

import de.polocloud.moduleapi.ModuleScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class ModuleDescriptorParserTest {

    private fun jarWithDescriptor(yaml: String?): File {
        val jar = Files.createTempFile("module-descriptor-test", ".jar").toFile()
        JarOutputStream(jar.outputStream()).use { out ->
            if (yaml != null) {
                out.putNextEntry(ZipEntry("module.yml"))
                out.write(yaml.toByteArray())
                out.closeEntry()
            }
        }
        return jar
    }

    @Test
    fun `parses a full descriptor`() {
        val jar = jarWithDescriptor(
            """
            name: cloudflare
            version: 1.2.3
            main: de.polocloud.modules.cloudflare.CloudflareModule
            description: Registers proxies with Cloudflare
            authors: [alice, bob]
            scope: single_active
            api-version: 3.0.0
            depends: [core]
            soft-depends: [metrics]
            dependencies: [com.squareup.okhttp3:okhttp:4.12.0]
            """.trimIndent()
        )

        val descriptor = ModuleDescriptorParser.parse(jar)

        assertEquals("cloudflare", descriptor.name)
        assertEquals("1.2.3", descriptor.version)
        assertEquals("de.polocloud.modules.cloudflare.CloudflareModule", descriptor.main)
        assertEquals("Registers proxies with Cloudflare", descriptor.description)
        assertEquals(listOf("alice", "bob"), descriptor.authors)
        assertEquals(ModuleScope.SINGLE_ACTIVE, descriptor.scope)
        assertEquals("3.0.0", descriptor.apiVersion)
        assertEquals(listOf("core"), descriptor.depends)
        assertEquals(listOf("metrics"), descriptor.softDepends)
        assertEquals(listOf("com.squareup.okhttp3:okhttp:4.12.0"), descriptor.dependencies)
    }

    @Test
    fun `defaults scope to EVERY_NODE and other optional fields to empty`() {
        val jar = jarWithDescriptor(
            """
            name: minimal
            main: de.polocloud.modules.minimal.MinimalModule
            """.trimIndent()
        )

        val descriptor = ModuleDescriptorParser.parse(jar)

        assertEquals(ModuleScope.EVERY_NODE, descriptor.scope)
        assertEquals("1.0.0", descriptor.version)
        assertEquals(emptyList<String>(), descriptor.depends)
        assertEquals(emptyList<String>(), descriptor.softDepends)
        assertEquals(null, descriptor.apiVersion)
    }

    @Test
    fun `throws when module yml is missing from the jar`() {
        val jar = jarWithDescriptor(null)

        assertThrows(IllegalStateException::class.java) { ModuleDescriptorParser.parse(jar) }
    }

    @Test
    fun `throws when name is missing`() {
        val jar = jarWithDescriptor("main: de.polocloud.modules.minimal.MinimalModule")

        assertThrows(IllegalStateException::class.java) { ModuleDescriptorParser.parse(jar) }
    }

    @Test
    fun `throws when main is missing`() {
        val jar = jarWithDescriptor("name: minimal")

        assertThrows(IllegalStateException::class.java) { ModuleDescriptorParser.parse(jar) }
    }

    @Test
    fun `throws on an unknown scope value`() {
        val jar = jarWithDescriptor(
            """
            name: minimal
            main: de.polocloud.modules.minimal.MinimalModule
            scope: NOT_A_REAL_SCOPE
            """.trimIndent()
        )

        assertThrows(IllegalStateException::class.java) { ModuleDescriptorParser.parse(jar) }
    }
}
