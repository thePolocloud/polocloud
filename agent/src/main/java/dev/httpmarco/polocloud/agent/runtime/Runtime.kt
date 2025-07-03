package dev.httpmarco.polocloud.agent.runtime

import dev.httpmarco.polocloud.agent.runtime.docker.DockerRuntime
import dev.httpmarco.polocloud.agent.runtime.k8s.KubernetesRuntime
import dev.httpmarco.polocloud.agent.runtime.local.LocalRuntime
import dev.httpmarco.polocloud.agent.services.Service

interface Runtime {

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
                KubernetesRuntime(),
                DockerRuntime(),
                LocalRuntime() // Fallback if others are not runnable
            ).firstOrNull { it.runnable() } ?: LocalRuntime()
            runtime.boot() // Boot the runtime if necessary
            return runtime
        }
    }

    fun boot() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary bootstrapping or initialization.
    }

    /**
     * Check if the runtime is runnable.
     * This method should be overridden by the specific runtime implementations
     */
    fun runnable(): Boolean

    /**
     * Returns the current storage for the runtime.
     * Only for all service related operations.
     */
    fun serviceStorage(): RuntimeServiceStorage

    /**
     * Returns the current group storage for the runtime.
     * Only for all group related operations.
     */
    fun groupStorage(): RuntimeGroupStorage

    /**
     * Returns the current factory for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    fun factory(): RuntimeFactory<Service>

    /**
     * Returns the expender for the runtime.
     * This method should be overridden by the specific runtime implementations
     */
    fun expender(): RuntimeExpender<Service>

    /**
     * Shuts down the runtime.
     * This method can be overridden by specific runtime implementations
     * to perform any necessary shutdown operations.
     */
    fun shutdown() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary shutdown operations.
    }

    fun postInitialize() {
        // Default implementation does nothing.
        // This method can be overridden by specific runtime implementations
        // to perform any necessary post-initialization operations.
    }
}