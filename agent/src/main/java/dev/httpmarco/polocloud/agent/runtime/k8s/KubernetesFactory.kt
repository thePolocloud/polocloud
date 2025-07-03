package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.runtime.RuntimeFactory

class KubernetesFactory : RuntimeFactory<KubernetesService> {
    override fun bootApplication(service: KubernetesService) {
        TODO("Not yet implemented")
    }

    override fun shutdownApplication(service: KubernetesService) {
        TODO("Not yet implemented")
    }
}