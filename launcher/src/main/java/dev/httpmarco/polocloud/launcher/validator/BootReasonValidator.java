package dev.httpmarco.polocloud.launcher.validator;

public interface BootReasonValidator extends BootValidator{

    /**
     * Called when {@link #isValid()} returns {@code false}.
     * <p>
     * Implementations should handle failure logic here, e.g., logging an error,
     * notifying the user, or stopping the launcher.
     */
    String failureMessage();

}
