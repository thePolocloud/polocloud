package dev.httpmarco.polocloud.launcher.boot;

import java.util.jar.Attributes;
import java.util.jar.JarFile;

public abstract class AbstractBoot implements Boot {

    public String mainClass() {
        try (var jarFile = new JarFile(bootFile())) {
            var manifest = jarFile.getManifest();
            if (manifest != null) {
                var mainAttributes = manifest.getMainAttributes();
                return mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
            } else {
                throw new RuntimeException(new NullPointerException("No main class detectable!"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
