package dev.httpmarco.polocloud.agent.runtime.docker

import dev.httpmarco.polocloud.agent.runtime.RuntimeExpender

class DockerExpender : RuntimeExpender<DockerService> {
    override fun executeCommand(
        service: DockerService,
        command: String
    ) {
        TODO("Not yet implemented")
    }

    override fun readLogs(
        service: DockerService,
        lines: Int
    ): List<String> {
        TODO("Not yet implemented")
    }
}