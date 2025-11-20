package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.runtime.RuntimeTemplateStorage
import dev.httpmarco.polocloud.shared.template.Template
import java.util.concurrent.CompletableFuture

class KubernetesRuntimeTemplateStorage : RuntimeTemplateStorage<Template, KubernetesService> {
    override fun findAll(): List<Template> {
        TODO("Not yet implemented")
    }

    override fun findAllAsync(): CompletableFuture<List<Template>> {
        TODO("Not yet implemented")
    }

    override fun find(name: String): Template? {
        TODO("Not yet implemented")
    }

    override fun findAsync(name: String): CompletableFuture<Template?> {
        TODO("Not yet implemented")
    }

    override fun availableTemplates(): List<Template> {
        TODO("Not yet implemented")
    }

    override fun bindTemplate(service: KubernetesService) {
        TODO("Not yet implemented")
    }

    override fun saveTemplate(
        template: Template,
        service: KubernetesService
    ) {
        TODO("Not yet implemented")
    }

    override fun templates(service: KubernetesService): List<Template> {
        TODO("Not yet implemented")
    }

    override fun create(name: String): Template {
        TODO("Not yet implemented")
    }

    override fun delete(template: Template) {
        TODO("Not yet implemented")
    }

    override fun update(template: Template, newName: String) {
        TODO("Not yet implemented")
    }

    override fun reload() {
        TODO("Not yet implemented")
    }

}