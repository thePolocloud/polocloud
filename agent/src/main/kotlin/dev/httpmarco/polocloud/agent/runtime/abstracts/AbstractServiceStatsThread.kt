package dev.httpmarco.polocloud.agent.runtime.abstracts

import dev.httpmarco.polocloud.agent.Agent

abstract class AbstractServiceStatsThread<T> : Thread("polocloud-local-cpu-detection") {

    @Suppress("UNCHECKED_CAST")
    override fun run() {
        while (true) {
            Agent.runtime.serviceStorage().findAll().forEach {
                detectService(it as T)
            }

            try {
                sleep(1000)
            } catch (_: InterruptedException) {
                interrupt()
                break
            }
        }
    }

    abstract fun detectService(service: T)
}