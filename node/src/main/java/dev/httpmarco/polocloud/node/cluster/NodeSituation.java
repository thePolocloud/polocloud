package dev.httpmarco.polocloud.node.cluster;

public enum NodeSituation {

    INITIALIZE,
    SYNC,
    RECHEABLE,
    STOPPING,
    STOPPED;

    public boolean isStopping() {
        return this == STOPPED || this == STOPPING;
    }
}