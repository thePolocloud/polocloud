package dev.httpmarco.polocloud.agent.configuration

import kotlinx.serialization.Serializable

@Serializable
data class AgentConfig(val processTerminationIdleSeconds : Int = 0, val maxQueueProcesses : Int = 0) {

}