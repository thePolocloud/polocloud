package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.agent.utils.Reloadable
import dev.httpmarco.polocloud.shared.template.SharedTemplateProvider
import dev.httpmarco.polocloud.shared.template.Template

interface RuntimeTemplateStorage<T : Template, out S : AbstractService> : SharedTemplateProvider<T>, Reloadable {

    fun availableTemplates() : List<Template>

    fun bindTemplate(service: @UnsafeVariance S)

    fun saveTemplate(template: Template, service: @UnsafeVariance S)

    fun templates(service: @UnsafeVariance S): List<Template>

    fun create(name: String): T

    fun delete(template: T)

    fun update(template: T, newName: String)

}