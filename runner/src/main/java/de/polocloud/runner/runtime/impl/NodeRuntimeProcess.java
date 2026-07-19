package de.polocloud.runner.runtime.impl;

import de.polocloud.runner.runtime.AbstractRuntimeProcess;

import java.util.Arrays;
import java.util.List;

public final class NodeRuntimeProcess extends AbstractRuntimeProcess {
    @Override
    protected String getArtifactId() {
        return "node";
    }

    @Override
    protected String getName() {
        return "PoloCloud Node";
    }

    @Override
    protected List<String> getRequiredModules() {
        return Arrays.asList(
                "common",
                "proto",
                "shared",
                "service-sdk",
                "updater"
        );
    }
}
