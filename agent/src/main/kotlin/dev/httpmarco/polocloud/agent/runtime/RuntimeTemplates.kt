package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.services.Service

interface RuntimeTemplates<out S : Service> {

    fun bindTemplate(service: @UnsafeVariance S)

    fun saveTemplate(template : String, service: @UnsafeVariance S)

}