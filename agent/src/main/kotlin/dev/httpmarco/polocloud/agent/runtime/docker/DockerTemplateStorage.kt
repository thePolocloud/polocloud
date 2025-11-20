package dev.httpmarco.polocloud.agent.runtime.docker

import com.github.dockerjava.api.DockerClient
import dev.httpmarco.polocloud.agent.runtime.RuntimeTemplateStorage
import dev.httpmarco.polocloud.shared.template.Template
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class DockerTemplateStorage(val client: DockerClient) : RuntimeTemplateStorage<DockerImage, DockerService> {

    private val executor = Executors.newCachedThreadPool()
    private val PREFIX = "polocloud/"
    private val LABEL_MANAGED = "polocloud.managed"

    override fun availableTemplates(): List<Template> {
        return client.listImagesCmd().exec()
            .filter { img -> img.labels?.get(LABEL_MANAGED) == "true" }
            .flatMap { it.repoTags?.toList() ?: emptyList() }
            .filter { it.startsWith(PREFIX) }
            .map { Template(it.removePrefix(PREFIX)) }
    }

    override fun bindTemplate(service: DockerService) {
        // todo
    }

    override fun saveTemplate(template: Template, service: DockerService) {
        /*
        val imageId = client.commitCmd(service.containerId)
            .withRepository("polocloud/${template.name}")
            .withTag("latest")
            .exec()
            TODO
         */
    }

    override fun templates(service: DockerService): List<Template> {
        TODO("Not yet implemented")
    }

    override fun create(name: String): DockerImage {
        TODO("Not yet implemented")
    }

    override fun delete(template: DockerImage) {
        TODO("Not yet implemented")
    }

    override fun update(template: DockerImage, newName: String) {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<DockerImage> {
        return client.listImagesCmd().exec()
            .filter { it.labels?.get(LABEL_MANAGED) == "true" }
            .flatMap { it.repoTags?.toList() ?: emptyList() }
            .filter { it.startsWith(PREFIX) }
            .map { DockerImage(client, it.removePrefix(PREFIX)) }
    }

    override fun findAllAsync(): CompletableFuture<List<DockerImage>> {
        return CompletableFuture.completedFuture(findAll())
    }

    override fun find(name: String): DockerImage {
        TODO("Not yet implemented")
    }

    override fun findAsync(name: String): CompletableFuture<DockerImage?> {
        TODO("Not yet implemented")
    }

    override fun reload() {
        TODO("Not yet implemented")
    }
}