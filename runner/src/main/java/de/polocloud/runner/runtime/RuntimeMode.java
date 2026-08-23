package de.polocloud.runner.runtime;

public enum RuntimeMode {
    CLI,
    NODE,
    /**
     * Downloads the bootstrap libraries (kotlin, log4j, slf4j) and extracts the embedded
     * module jars into the runtime cache, then exits without booting the node or CLI. Meant
     * to be run as a build step (e.g. {@code RUN java -jar polocloud-runner.jar --prime-cache}
     * in a Dockerfile) so the resulting image already has a populated cache and never needs
     * to reach Maven Central when the container actually starts.
     */
    PRIME_CACHE
}
