package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.runtime.RuntimeExpender

class KubernetesExpender : RuntimeExpender<KubernetesService> {

    override fun executeCommand(
        service: KubernetesService,
        command: String
    ) {
        TODO("Not yet implemented")
    }

    override fun readLogs(
        service: KubernetesService,
        lines: Int
    ): List<String> {
        TODO("Not yet implemented")
    }
}