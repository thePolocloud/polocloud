package dev.httpmarco.polocloud.cli.validator;

import dev.httpmarco.polocloud.dependency.platform.Dependency;

import java.util.ArrayList;
import java.util.List;

public final class DependencyStateCheck implements BootReasonValidator {

    private final List<Dependency> failedDependencies = new ArrayList<>();

    @Override
    public boolean isValid() {
        List<Dependency> dependencies = new ArrayList<>();

        // todo read manifest dependency file

        for (Dependency dependency : dependencies) {
            if (!dependency.isAvailable()) {
                failedDependencies.add(dependency);
            }
        }

        return true;
    }

    @Override
    public String failureMessage() {
        StringBuilder builder = new StringBuilder("Some dependencies are missing or corrupted: ");
        failedDependencies.forEach(it -> (builder.append("\n     - ").append(it.toString())).append(" ").append("(Reason: ").append(it.status()).append(")"));
        return builder.toString();
    }
}
