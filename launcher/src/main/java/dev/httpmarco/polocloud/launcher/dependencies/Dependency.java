package dev.httpmarco.polocloud.launcher.dependencies;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;

public class Dependency {

    private final String artifactId;
    private final String url;
    private final String checksum;
    private final String version;

    private File downloadedFile;

    public Dependency(String artifactId, String url, String checksum, String version) {
        this.artifactId = artifactId;
        this.url = url;
        this.checksum = checksum;
        this.version = version;
    }

    private boolean validChecksum() {
        if (downloadedFile == null || !downloadedFile.exists() || checksum == null) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(downloadedFile)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String calculated = sb.toString().toLowerCase();
            return calculated.equals(checksum.toLowerCase());

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public void download() {
        final int maxAttempts = 3;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                File depsDir = new File(System.getProperty("user.dir"), ".polo/dependencies");
                if (!depsDir.exists() && !depsDir.mkdirs()) {
                    throw new RuntimeException("Could not create dependencies directory: " + depsDir.getAbsolutePath());
                }

                downloadedFile = new File(depsDir, artifactId + "-" + version + ".jar");

                URL downloadUrl = new URL(url);
                try (InputStream in = downloadUrl.openStream();
                     FileOutputStream out = new FileOutputStream(downloadedFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                if (!validChecksum()) {
                    System.err.println("Attempt " + attempt + ": Checksum invalid for " + this);
                    downloadedFile.delete();
                    downloadedFile = null;
                    if (attempt < maxAttempts) {
                        Thread.sleep(100);
                    } else {
                        System.err.println("Failed to download dependency after " + maxAttempts + " attempts: " + this);
                        break;
                    }
                } else {
                    System.out.println("Dependency downloaded successfully: " + this);
                    break;
                }

            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed for " + this + ": " + e.getMessage());
                downloadedFile = null;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                } else {
                    System.err.println("Failed to download dependency after " + maxAttempts + " attempts: " + this);
                }
            }
        }
    }


    public boolean isAvailable() {
        return downloadedFile != null && downloadedFile.exists() && validChecksum();
    }

    public String status() {
        if(downloadedFile == null || !downloadedFile.exists()) {
            return "Not downloaded";
        }

        if(!validChecksum()) {
            return "Invalid checksum";
        }

        return "Healthy";
    }

    @Override
    public String toString() {
        return artifactId + "-" + version;
    }
}
