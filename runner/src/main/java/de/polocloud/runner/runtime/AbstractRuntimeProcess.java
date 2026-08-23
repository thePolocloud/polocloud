package de.polocloud.runner.runtime;

import de.polocloud.runner.PolocloudParameters;
import de.polocloud.runner.classloader.PolocloudClassLoader;
import de.polocloud.runner.expender.BootstrapLibraries;
import de.polocloud.runner.expender.ExpenderRuntimeCache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractRuntimeProcess implements RuntimeProcess {

    @Override
    public int start() {
        try {
            prepareRuntimeEnvironment();
            invokeMain(createClassLoader());
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to start " + getName());
            e.printStackTrace(System.err);
        }
        return 1;
    }

    protected abstract String getArtifactId();

    protected abstract String getName();

    protected List<String> getRequiredModules() {
        return Collections.emptyList();
    }

    private void prepareRuntimeEnvironment() throws IOException, URISyntaxException {
        ExpenderRuntimeCache.migrateCacheFiles();
        BootstrapLibraries.ensurePresent();
    }

    private PolocloudClassLoader createClassLoader() throws MalformedURLException {
        List<URL> urls = new ArrayList<>();

        for (Path path : getClasspath()) {
            urls.add(path.toUri().toURL());
        }

        return new PolocloudClassLoader(urls.toArray(new URL[0]));
    }

    private List<Path> getClasspath() {
        List<Path> elements = new ArrayList<>();

        // bootstrap: kotlin + logging (must be present before node main is invoked)
        elements.add(PolocloudParameters.bootKotlin());
        elements.add(PolocloudParameters.bootLog4jApi());
        elements.add(PolocloudParameters.bootLog4jCore());
        elements.add(PolocloudParameters.bootLog4jSlf4jImpl());
        elements.add(PolocloudParameters.bootSlf4jApi());

        for (String module : getRequiredModules()) {
            elements.add(PolocloudParameters.expenderRuntimeCache(module));
        }

        elements.add(PolocloudParameters.expenderRuntimeCache(getArtifactId()));

        return elements;
    }

    private void invokeMain(ClassLoader classLoader) throws ReflectiveOperationException, InterruptedException {
        String mainClassName = resolveMainClassName();

        // attach right boot file name
        System.setProperty(PolocloudParameters.RUNTIME_PATH, String.valueOf(Objects.requireNonNull(PolocloudParameters.expenderRuntimeCache(getArtifactId()))));
        System.setProperty(PolocloudParameters.COMMON_PATH, String.valueOf(Objects.requireNonNull(PolocloudParameters.expenderRuntimeCache("common"))));
        System.setProperty(PolocloudParameters.PROTO_PATH, String.valueOf(Objects.requireNonNull(PolocloudParameters.expenderRuntimeCache("proto"))));
        System.setProperty(PolocloudParameters.SHARED_PATH, String.valueOf(Objects.requireNonNull(PolocloudParameters.expenderRuntimeCache("shared"))));


        Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Thread thread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
        thread.join();
    }

    private String resolveMainClassName() {
        return Objects.requireNonNull(
                ExpenderRuntimeCache.findElementByArtifactId(getArtifactId()),
                getName() + " artifact not found"
        ).mainClass();
    }
}
