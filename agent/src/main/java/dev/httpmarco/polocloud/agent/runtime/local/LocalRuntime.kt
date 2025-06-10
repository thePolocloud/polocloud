package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.runtime.Runtime
import dev.httpmarco.polocloud.agent.runtime.RuntimeServiceStorage
import dev.httpmarco.polocloud.agent.runtime.local.terminal.Jline3Terminal

class LocalRuntime : Runtime {

    private val runtimeGroupStorage = LocalRuntimeGroupStorage()
    private lateinit var terminal: Jline3Terminal

    override fun boot() {
        terminal = Jline3Terminal()
    }

    override fun runnable(): Boolean {
        return true // LocalRuntime is always runnable
    }

    override fun serviceStorage(): RuntimeServiceStorage {
        TODO("Not yet implemented")
    }

    override fun groupStorage() = runtimeGroupStorage

    override fun shutdown() {
        this.terminal.shutdown()
    }

    override fun postInitialize() {
        this.terminal.jLine3Reading.start()
    }
}