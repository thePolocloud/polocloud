package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.services.Service

class LocalRuntimeQueue : Thread("polocloud-local-runtime-queue") {

    override fun run() {
        while (!isInterrupted) {
            Agent.instance.runtime.groupStorage().items().forEach {
                for (n in 0 until requiredServersThatStart(it)) {

                    val service = Service(it)

                    Agent.instance.runtime.serviceStorage().deployService(service)
                    Agent.instance.runtime.factory().bootApplication(service)
                }
            }
            sleep(1000)
        }
    }

    private fun requiredServersThatStart(group : Group): Int {
        return (group.data.minOnlineService - group.serviceCount()).coerceAtLeast(0);
    }
}