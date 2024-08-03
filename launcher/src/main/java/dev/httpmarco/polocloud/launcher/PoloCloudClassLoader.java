package dev.httpmarco.polocloud.launcher;

import java.net.URL;
import java.net.URLClassLoader;

public final class PoloCloudClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PoloCloudClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader());
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
