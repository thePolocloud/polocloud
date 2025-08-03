package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.shutdownProcess
import dev.httpmarco.polocloud.platforms.PlatformType

class LocalRuntimeQueue : Thread("polocloud-local-runtime-queue") {


    override fun run() {
        try {
            val runtime = Agent.runtime

            while (!isInterrupted && !shutdownProcess()) {
                runtime.groupStorage().items()
                    .filter { requiredServersThatStart(it) > 0 }
                    .forEach { group ->
                        val required = requiredServersThatStart(group)

                        repeat(required) {
                            val index = findIndex(group)
                            val service = when (group.platform().type) {
                                PlatformType.PROXY -> LocalService(group, index, "0.0.0.0")
                                else -> LocalService(group, index)
                            }

                            runtime.serviceStorage().deployService(service)
                            runtime.factory().bootApplication(service)
                        }
                    }

                sleep(1000)
            }
        } catch (_: InterruptedException) {
            // The Thread was interrupted, we can exit gracefully
        } catch (e: Throwable) {
            logger.throwable(e)
        }
    }

    private fun requiredServersThatStart(group: Group): Int {
        return (group.data.minOnlineService - group.serviceCount()).coerceAtLeast(0)
    }

    private fun findIndex(group: Group): Int {
        var id = 1
        while (Agent.runtime.serviceStorage().items().stream()
                .anyMatch { it.group == group && it.id == id }
        ) {
            id++
        }
        return id
    }
}