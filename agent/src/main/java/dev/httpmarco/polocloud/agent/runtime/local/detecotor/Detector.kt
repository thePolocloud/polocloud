package dev.httpmarco.polocloud.agent.runtime.local.detecotor

interface Detector {

    fun tick()

    fun cycleLife() : Long

}