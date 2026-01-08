package dev.httpmarco.polocloud.runner.validator;

/**
 * Represents a boot-time validator for the PoloCloud launcher.
 * <p>
 * Implementations should provide logic to check if a certain condition
 * is met before the launcher continues starting up.
 */
public interface BootValidator {

    /**
     * Checks whether the validation passes.
     *
     * @return {@code true} if the validation succeeds, {@code false} otherwise
     */
    boolean isValid();

}
