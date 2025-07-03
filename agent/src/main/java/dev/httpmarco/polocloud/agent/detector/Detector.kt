package dev.httpmarco.polocloud.agent.detector

interface Detector {

    fun tick()

    fun cycleLife() : Long

}