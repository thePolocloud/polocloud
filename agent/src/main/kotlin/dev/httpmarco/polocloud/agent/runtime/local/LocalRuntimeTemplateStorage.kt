package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.runtime.RuntimeTemplateStorage
import dev.httpmarco.polocloud.common.filesystem.copyDirectory
import dev.httpmarco.polocloud.common.filesystem.deleteDirectory
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.v1.GroupType
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createDirectories

class LocalRuntimeTemplateStorage : RuntimeTemplateStorage<LocalTemplate, LocalService> {

    private val cachedTemplates = mutableListOf<LocalTemplate>()

    init {
        this.reload()
    }

    override fun availableTemplates() = cachedTemplates

    override fun bindTemplate(service: LocalService) {
        service.templates.forEach {
            val sourcePath = templatePath(it)
            if (!Files.exists(sourcePath)) {
                sourcePath.createDirectories()
                // no template found, create empty directory
            }
            copyDirectory(sourcePath, service.path)
        }
    }

    override fun saveTemplate(template: Template, service: LocalService) {
        copyDirectory(service.path, templatePath(template))
    }

    override fun templates(service: LocalService): List<Template> {
        return this.cachedTemplates.toList()
    }

    override fun create(name: String): LocalTemplate {
        if (find(name) != null) {
            return find(name)!!
        }

        val template = LocalTemplate(name)
        val sourcePath = templatePath(template)

        if (!Files.exists(sourcePath)) {
            sourcePath.createDirectories()
            this.cachedTemplates.add(template)
        }

        return template
    }

    override fun delete(template: LocalTemplate) {
        this.cachedTemplates.removeIf { it.name == template.name }
        deleteDirectory(templatePath(template))
    }

    override fun update(template: LocalTemplate, newName: String) {
        val sourcePath = templatePath(template)
        val targetPath = LOCAL_TEMPLATE_PATH.resolve(newName)

        if (!Files.exists(sourcePath)) {
            return
        }
        if (Files.exists(targetPath)) {
            logger.warn("Template $newName already exists")
            return
        }

        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE)
            this.cachedTemplates.removeIf { it.name == template.name }
            this.cachedTemplates.add(LocalTemplate(newName))
        } catch (e: IOException) {
            logger.warn("Cannot rename template ${template.name} to $newName: ${e.message}")
        }
    }

    override fun findAll() = this.cachedTemplates

    override fun findAllAsync(): CompletableFuture<List<LocalTemplate>> = CompletableFuture.completedFuture(findAll())

    override fun find(name: String) = this.cachedTemplates.find { it.name == name }

    override fun findAsync(name: String): CompletableFuture<LocalTemplate?> =
        CompletableFuture.completedFuture(find(name))

    private fun templatePath(template: Template) = LOCAL_TEMPLATE_PATH.resolve(template.name)

    override fun reload() {
        this.cachedTemplates.clear()

        if (!Files.exists(LOCAL_TEMPLATE_PATH)) {
            Files.createDirectories(LOCAL_TEMPLATE_PATH)
        }

        Files.list(LOCAL_TEMPLATE_PATH).forEach { dir ->
            if (Files.isDirectory(dir)) {
                val name = dir.fileName.toString()
                this.cachedTemplates.add(LocalTemplate(name))
            }
        }

        create("EVERY")
        create("EVERY_FALLBACK")

        GroupType.entries.forEach {
            create("EVERY_${it.name}")
        }
    }
}
