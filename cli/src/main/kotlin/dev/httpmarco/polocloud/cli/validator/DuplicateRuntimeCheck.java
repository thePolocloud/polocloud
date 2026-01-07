package dev.httpmarco.polocloud.cli.validator;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

public final class DuplicateRuntimeCheck implements BootReasonValidator {

    private static final String LOCK_FILE_NAME = "polocloud-launcher.lock";
    private FileChannel channel;
    private FileLock lock;
    private File lockFile;

    @Override
    @SuppressWarnings("resource")
    public boolean isValid() {
        try {
            lockFile = new File(System.getProperty("user.dir"), LOCK_FILE_NAME);
            channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();

            if (lock == null) {
                return false;
            }

            writePid();
            Runtime.getRuntime().addShutdownHook(new Thread(this::releaseLock));
            return true;

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    private void writePid() {
        try {
            byte[] pidBytes = getProcessId().getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.wrap(pidBytes);
            channel.truncate(0);

            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written <= 0) {
                    throw new IllegalStateException("Failed to write PID to lock file");
                }
            }

            channel.force(true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private String getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];
    }

    private void releaseLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (lockFile != null && lockFile.exists()) {
                if (!lockFile.delete()) {
                    System.err.println("Warning: Failed to delete lock file: " + lockFile.getAbsolutePath());
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String failureMessage() {
        return "Duplicated runtime process detected. Another instance is already running.";
    }
}
