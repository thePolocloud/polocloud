package dev.httpmarco.polocloud.runner;

import dev.httpmarco.polocloud.runner.validator.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs all boot-time validations for the PoloCloud launcher.
 * <p>
 * This includes checking the Java runtime version, working directory permissions,
 * and whether another instance of the launcher is already running.
 */
public final class PolocloudRuntimeBootValidator implements BootValidator {

    /**
     * List of all boot validators to run.
     */
    private final BootReasonValidator[] validators = new BootReasonValidator[]{
            new JavaRuntimeVersionCheck(),
            new FileSecurityCheck(),
            new DependencyStateCheck(),
            new DuplicateRuntimeCheck()
    };

    /**
     * Runs all validators sequentially. If any validator fails,
     *
     * @return {@code true} if all validators succeed, {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        List<BootReasonValidator> failedValidators = new ArrayList<>();

        for (BootReasonValidator validator : validators) {
            if (!validator.isValid()) {
                failedValidators.add(validator);
            }
        }

        if (!failedValidators.isEmpty()) {
            System.err.println("PoloCloud Launcher boot validation failed due to the following reasons: ");

            for (BootReasonValidator failedValidator : failedValidators) {
                System.err.println("  - " + failedValidator.failureMessage());
            }

            System.err.println(" ");
            System.err.println("Shutting down PoloCloud Launcher due to failed boot validation.");
            System.err.println("Please check the error messages above for more information.");
            return false;
        }
        return true;
    }
}
