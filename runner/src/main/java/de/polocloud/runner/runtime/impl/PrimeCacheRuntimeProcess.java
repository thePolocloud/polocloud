package de.polocloud.runner.runtime.impl;

import de.polocloud.runner.expender.BootstrapLibraries;
import de.polocloud.runner.expender.ExpenderRuntimeCache;
import de.polocloud.runner.runtime.RuntimeProcess;

/**
 * Populates the runtime cache (bootstrap libraries + embedded module jars) and exits,
 * without booting the node or CLI. See {@link de.polocloud.runner.runtime.RuntimeMode#PRIME_CACHE}.
 */
public final class PrimeCacheRuntimeProcess implements RuntimeProcess {

    @Override
    public int start() {
        try {
            ExpenderRuntimeCache.migrateCacheFiles();
            BootstrapLibraries.ensurePresent();
            System.out.println("[Bootstrap] Cache primed - bootstrap libraries and module jars are on disk.");
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to prime runtime cache");
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
