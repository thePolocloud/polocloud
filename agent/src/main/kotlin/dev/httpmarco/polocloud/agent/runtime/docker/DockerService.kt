package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.agent.utils.IndexDetector
import dev.httpmarco.polocloud.agent.utils.PortDetector
import dev.httpmarco.polocloud.shared.service.ServiceInformation
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.v1.groups.GroupType
import dev.httpmarco.polocloud.v1.services.ServiceState


class DockerService(
    name: String,
    index: Int,
    state: ServiceState,
    platformType: GroupType,
    environment: HashMap<String, String>,
    host: String,
    port: Int,
    templates: List<Template>,
    information: ServiceInformation,
    minMemory: Int,
    maxMemory: Int
) : AbstractService(
    name,
    index,
    state,
    platformType,
    environment,
    host,
    port,
    templates,
    information,
    minMemory,
    maxMemory
) {

    var containerId: String? = null

    constructor(group: AbstractGroup) : this(
        group.name,
        IndexDetector.findIndex(group),
        ServiceState.PREPARING,
        group.platform().type,
        hashMapOf(),
        if (group.isProxy()) "0.0.0.0" else "127.0.0.1",
        PortDetector.nextPort(group),
        group.templates,
        ServiceInformation(System.currentTimeMillis()),
        group.minMemory,
        group.maxMemory
    )

    fun changeToContainerHostname(hostname: String) {
        this.hostname  = hostname
    }
}
