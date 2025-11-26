package dev.httpmarco.polocloud.agent.runtime.abstracts

abstract class Track {

    protected val threads = mutableListOf<Thread>()

    abstract fun start()

    fun close() {
        threads.forEach { it.interrupt() }
    }
}