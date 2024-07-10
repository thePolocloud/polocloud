package dev.httpmarco.polocloud.base.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.module.CloudModule;
import dev.httpmarco.polocloud.api.module.LoadedModule;
import dev.httpmarco.polocloud.api.module.ModuleMetadata;
import dev.httpmarco.polocloud.runner.RunnerBootstrap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarFile;

@Getter
@Accessors(fluent = true)
public class ModuleManager {

    private static final Path MODULE_PATH = Path.of("./local/modules/");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<LoadedModule> loadedModules = new CopyOnWriteArrayList<>();
    private final ClassLoader parentClassLoader;

    @SneakyThrows
    public ModuleManager() {
        this.parentClassLoader = RunnerBootstrap.LOADER;
        if (!Files.exists(MODULE_PATH)) Files.createDirectory(MODULE_PATH);
    }

    public void loadAllUnloadedModules() {
        var moduleFiles = getAllModuleJarFiles();
        loadModuleListFromFiles(moduleFiles);
    }

    public void unloadAllModules() {
        this.loadedModules.forEach(this::unloadModule);
    }

    public List<LoadedModule> getLoadedModules() {
        return new ArrayList<>(loadedModules);
    }

    private void loadModuleListFromFiles(List<File> files) {
        files.forEach(this::loadModuleFileContent);
    }

    @SneakyThrows
    private void loadModuleFileContent(File file) {
        var metadata = loadModuleMetadata(file);
        var cloudModule = loadModule(file, metadata.main());
        if (cloudModule == null) {
            CloudAPI.instance().logger().warn("Failed to load Module:\"" + metadata.name() + "\" by " + metadata.author());
            return;
        }

        var classLoader = (URLClassLoader) cloudModule.getClass().getClassLoader();
        var loadedModule = new LoadedModule(cloudModule, classLoader, metadata);
        loadedModules.add(loadedModule);
        cloudModule.onEnable();
    }

    @SneakyThrows
    private ModuleMetadata loadModuleMetadata(File file) {
        try (var jarFile = new JarFile(file)) {
            var reader = new InputStreamReader(jarFile.getInputStream(jarFile.getJarEntry("module.json")));
            return GSON.fromJson(reader, ModuleMetadata.class);
        }
    }

    private CloudModule loadModule(File file, String mainClass) {
        try {
            var jarUrl = file.toURI().toURL();
            var classLoader = new URLClassLoader(new URL[]{jarUrl}, this.parentClassLoader);
            var clazz = classLoader.loadClass(mainClass);

            if (!CloudModule.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + mainClass + " does not implement CloudModule");
            }

            return (CloudModule) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void unloadModule(LoadedModule loadedModule) {
        loadedModule.cloudModule().onDisable();
        loadedModule.moduleClassLoader().close();
        this.loadedModules.remove(loadedModule);
        CloudAPI.instance().logger().info( "Successfully stopped Module: \"" + loadedModule.metadata().name() + "\"");
    }

    private List<File> getAllModuleJarFiles() {
        var files = MODULE_PATH.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            return List.of(files);
        } else {
            return List.of();
        }
    }

}
