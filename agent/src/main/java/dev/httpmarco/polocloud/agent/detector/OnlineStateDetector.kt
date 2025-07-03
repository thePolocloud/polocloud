package dev.httpmarco.polocloud.agent.detector

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.agent.services.Service
import java.net.Socket

class OnlineStateDetector : Detector {

    override fun tick() {
        Agent.instance.runtime.serviceStorage().items().forEach {

            if(it.state !== Service.State.STARTING) {
                return
            }

            if(pingService(it)) {
                // change state to ONLINE
                it.state = Service.State.ONLINE
                logger.info("The service ${it.name()} is now online.")
            }
        }
    }

    override fun cycleLife(): Long {
        return 2000
    }

    private fun pingService(service: Service): Boolean {
        try {
            Socket("localhost", service.port).use { socket ->
                return socket.isConnected
            }
        } catch (_: Exception) {
            return false
        }
    }
}