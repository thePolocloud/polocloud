package dev.httpmarco.polocloud.modules.rest.controller.impl.v3.controller.template

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntimeTemplateStorage
import dev.httpmarco.polocloud.agent.runtime.local.LocalTemplate
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.modules.rest.controller.Controller
import dev.httpmarco.polocloud.modules.rest.controller.defaultResponse
import dev.httpmarco.polocloud.modules.rest.controller.impl.v3.model.template.CreateTemplateModel
import dev.httpmarco.polocloud.modules.rest.controller.impl.v3.model.template.EditTemplateModel
import dev.httpmarco.polocloud.modules.rest.controller.methods.Request
import dev.httpmarco.polocloud.modules.rest.controller.methods.RequestType
import dev.httpmarco.polocloud.shared.template.Template
import io.javalin.http.Context

class TemplateController : Controller("/template") {

    @Request(requestType = RequestType.GET, path = "s/list", permission = "polocloud.templates.list")
    fun listTemplates(context: Context) {
        val services = Agent.serviceProvider().findAll()

        val templates = mutableSetOf<Template>()
        services.forEach { service ->
            templates += Agent.runtime.templateStorage().templates(service as AbstractService)
        }

        val data = JsonArray().apply {
            templates.forEach { template ->
                add(JsonObject().apply {
                    addProperty("name", template.name)
                    addProperty("size", template.size())
                })
            }
        }

        context.defaultResponse(200, data = data)
    }

    @Request(requestType = RequestType.POST, path = "/", permission = "polocloud.templates.create")
    fun createTemplate(context: Context) {
        val model = context.parseBodyOrBadRequest<CreateTemplateModel>() ?: return
        if (!context.validate(model.name.isNotBlank(), "Template name is required")) return

        val searchedTemplate = Agent.runtime.templateStorage().find(model.name)
        if (searchedTemplate != null) {
            context.defaultResponse(400, "Template already exists")
            return
        }

        val runtimeTemplateStorage = Agent.runtime.templateStorage()
        when (runtimeTemplateStorage) {
            is LocalRuntimeTemplateStorage -> {
                runtimeTemplateStorage.create(model.name)
            }

            // TODO DOCKER AND K8S IMPLEMENTATION

            else -> {
                context.defaultResponse(500, "Unsupported template runtime")
                return
            }
        }

        context.defaultResponse(202, "Creating template")
    }

    @Request(requestType = RequestType.DELETE, path = "/{templateName}", permission = "polocloud.templates.delete")
    fun deleteTemplate(context: Context) {
        val templateName = context.pathParam("templateName")
        val template = Agent.runtime.templateStorage().find(templateName)

        if (template == null) {
            context.defaultResponse(400, "Template could not be found")
            return
        }

        val runtimeTemplateStorage = Agent.runtime.templateStorage()
        when (runtimeTemplateStorage) {
            is LocalRuntimeTemplateStorage -> runtimeTemplateStorage.delete(template as LocalTemplate)

            // TODO DOCKER AND K8S IMPLEMENTATION

            else -> {
                context.defaultResponse(500, "Unsupported template runtime")
                return
            }
        }

        context.defaultResponse(204)
    }

    @Request(requestType = RequestType.PATCH, path = "/{templateName}", permission = "polocloud.templates.edit")
    fun editTemplate(context: Context) {
        val templateName = context.pathParam("templateName")
        val model = context.parseBodyOrBadRequest<EditTemplateModel>() ?: return

        if (!context.validate(model.name.isNotBlank(), "Template name is required")) return

        val template = Agent.runtime.templateStorage().find(templateName)
        if (template == null) {
            context.defaultResponse(400, "Template could not be found")
            return
        }

        val runtimeTemplateStorage = Agent.runtime.templateStorage()

        when (runtimeTemplateStorage) {
            is LocalRuntimeTemplateStorage -> runtimeTemplateStorage.update(template as LocalTemplate, model.name)

            else -> {
                context.status(500).json(message("Unsupported template runtime"))
                return
            }
        }

        context.defaultResponse(202, "Template edited")
    }
}