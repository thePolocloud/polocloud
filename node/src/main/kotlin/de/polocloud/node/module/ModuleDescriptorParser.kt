package de.polocloud.node.module

import de.polocloud.moduleapi.ModuleDescriptor
import de.polocloud.moduleapi.ModuleScope
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.jar.JarFile

/** Reads the `module.yml` bundled at the root of a module jar into a [ModuleDescriptor]. */
object ModuleDescriptorParser {

    private const val DESCRIPTOR_ENTRY = "module.yml"

    fun parse(jarFile: File): ModuleDescriptor {
        JarFile(jarFile).use { jar ->
            val entry = jar.getEntry(DESCRIPTOR_ENTRY)
                ?: throw IllegalStateException("'${jarFile.name}' has no $DESCRIPTOR_ENTRY at its jar root")

            @Suppress("UNCHECKED_CAST")
            val data = jar.getInputStream(entry).use { Yaml().load<Map<String, Any?>>(it) }
                ?: emptyMap()

            val name = data["name"] as? String
                ?: throw IllegalStateException("'${jarFile.name}': module.yml is missing 'name'")
            val main = data["main"] as? String
                ?: throw IllegalStateException("'${jarFile.name}': module.yml is missing 'main'")
            val scopeRaw = data["scope"] as? String

            return ModuleDescriptor(
                name = name,
                version = data["version"] as? String ?: "1.0.0",
                main = main,
                description = data["description"] as? String ?: "",
                authors = (data["authors"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                scope = scopeRaw?.let {
                    ModuleScope.entries.find { scope -> scope.name.equals(it, ignoreCase = true) }
                        ?: throw IllegalStateException("'${jarFile.name}': unknown scope '$it' (expected one of ${ModuleScope.entries.joinToString { s -> s.name }})")
                } ?: ModuleScope.EVERY_NODE,
                depends = (data["depends"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                softDepends = (data["soft-depends"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                apiVersion = data["api-version"] as? String,
                dependencies = (data["dependencies"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            )
        }
    }
}
