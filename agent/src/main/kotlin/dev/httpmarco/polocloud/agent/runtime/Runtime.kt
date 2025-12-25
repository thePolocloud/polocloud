package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.runtime.abstracts.AbstractServiceStatsThread
import dev.httpmarco.polocloud.agent.runtime.docker.DockerRuntimeLoader
import dev.httpmarco.polocloud.agent.runtime.k8s.KubernetesRuntimeLoader
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntimeLoader
import dev.httpmarco.polocloud.agent.services.AbstractService

abstract class Runtime {

    private val started = System.currentTimeMillis()

    companion object {
        /**
         * Creates a new instance of the runtime based on the current environment.
         * The order of preference is:
         *   1. Kubernetes
         *   2. Docker
         *   3. Local (fallback)
         *
         * @return the most suitable [Runtime] implementation for the current environment.
         */
        fun create(): Runtime {
            val runtime = listOf(
                KubernetesRuntimeLoader(),
                DockerRuntimeLoader(),
                LocalRuntimeLoader() // Fallback if others are not runnable
            ).firstOrNull { it.runnable() } ?: LocalRuntimeLoader()
            return runtime.instance()
        }
    }

    /**
     * Boot order
     * 1. Initialize() - called before boot
     * 2. boot() - called after initialize
     * 3. Runnable() - check if the runtime is runnable
     * ...
     * 4. shutdown - called to shut down the runtime
     */
    open fun initialize() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary initialization.
        // it's called before the boot method

        // if nothing is done here, the boot method is called directly
        Agent.boot()
    }

    open fun prepareBoot() {
        // start the service stats thread
        serviceStatsThread().start()
        // call the boot method
        this.boot()
    }

    open fun boot() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary bootstrapping or initialization.
    }

    /**
     * Returns the current storage for the runtime.
     * Only for all service related operations.
     */
    abstract fun serviceStorage(): RuntimeServiceStorage<*>

    /**
     * Returns the current group storage for the runtime.
     * Only for all group related operations.
     */
    abstract fun groupStorage(): RuntimeGroupStorage

    /**
     * Returns the current factory for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    abstract fun factory(): RuntimeFactory<AbstractService>

    /**
     * Returns the expender for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    abstract fun expender(): RuntimeExpender<AbstractService>

    /**
     * Returns the templates for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    abstract fun templateStorage(): RuntimeTemplateStorage<*, AbstractService>

    /**
     * Returns the holder for the runtime configuration.
     */
    abstract fun configHolder(): RuntimeConfigHolder

    /**
     * Shuts down the runtime.
     * This method can be overridden by specific runtime implementations
     * to perform any necessary shutdown operations.
     */
    open fun shutdown() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary shutdown operations.
    }

    fun started(): Long = started

    /**
     * Sends a command to the runtime.
     * This method can be overridden by specific runtime implementations
     * to send commands to the runtime.
     */
    abstract fun sendCommand(command: String)

    /**
     * Detects the local address of the runtime environment.
     * This method should be overridden by specific runtime implementations
     */
    abstract fun detectLocalAddress() : String

    /**
     * Returns the service stats thread for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    abstract fun serviceStatsThread(): AbstractServiceStatsThread<*>
}