package dev.httpmarco.polocloud.cli.validator;

import java.io.File;

/**
 * Checks if the launcher has sufficient read/write permissions
 * in the working directory.
 */
public final class FileSecurityCheck implements BootReasonValidator {

    /**
     * The working directory of the launcher.
     */
    private static final File WORK_DIR = new File(System.getProperty("user.dir"));

    /**
     * Validates that the working directory exists and is readable and writable.
     *
     * @return {@code true} if the working directory is accessible, {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        return WORK_DIR.exists() && WORK_DIR.canRead() && WORK_DIR.canWrite();
    }

    /**
     * Called when {@link #isValid()} returns {@code false}.
     * Prints an error message to the user explaining the issue.
     */
    @Override
    public String failureMessage() {
        return "The application does not have sufficient permissions to read/write in the working directory: " + WORK_DIR.getAbsolutePath();
    }
}
