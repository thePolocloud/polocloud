package dev.httpmarco.polocloud.agent.runtime.k8s

import dev.httpmarco.polocloud.agent.logger
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.KubernetesClient

const val KUBE_IDENTIFIER = "polocloud"

fun init(client: KubernetesClient) {
    createNamespaceIfNotExists(client)
}


fun createNamespaceIfNotExists(client: KubernetesClient) {
    if (client.namespaces().withName(KUBE_IDENTIFIER).get() == null) {
        client.namespaces().resource(
            NamespaceBuilder()
                .withNewMetadata()
                .withName(KUBE_IDENTIFIER)
                .endMetadata()
                .build()
        ).serverSideApply()
        logger.info("Created namespace '$KUBE_IDENTIFIER' in Kubernetes cluster.")
    }
}