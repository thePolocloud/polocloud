/*
 * Copyright 2024 Mirco Lindenau | HttpMarco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.httpmarco.polocloud.api.dependencies;

import dev.httpmarco.polocloud.runner.RunnerBootstrap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarFile;

@Getter
@Accessors(fluent = true)
public final class Dependency {

    public static final String MAVEN_CENTRAL_REPO = "https://repo1.maven.org/maven2/%s/%s/%s/%s.jar";
    public static final String MAVEN_CENTRAL_SNAPSHOT_REPO = "https://s01.oss.sonatype.org/service/local/repositories/snapshots/content/%s/%s/%s/%s.jar";

    private final String dependencyLink;
    private final File file;

    public static void load(String groupId, String artifactoryId, String version) {
        load(groupId, artifactoryId, version, version, MAVEN_CENTRAL_REPO);
    }

    public static void load(String groupId, String artifactoryId, String version, String subversion, String repository) {
        new Dependency(groupId, artifactoryId, version, subversion, repository);
    }

    @SneakyThrows
    private Dependency(String groupId, String artifactoryId, String version, String subversion, String repository) {
        this.dependencyLink = repository.formatted(groupId.replace(".", "/"), artifactoryId, version, artifactoryId + "-" + subversion);

        var name = groupId + "." + artifactoryId + "." + version;
        var file = RunnerBootstrap.RUNNER.dependencyFolder().resolve(artifactoryId + "-" + version + ".jar");
        this.file = file.toFile();

        if (!Files.exists(file)) {
            System.out.println("Downloading dependency: " + name + "...");
            if (download(dependencyLink)) {
                System.out.println("Successfully downloading dependency " + name);
            } else {
                System.err.println("Cannot download dependency: " + name);
                System.exit(-1);
            }
        }
        if (RunnerBootstrap.INSTRUMENTATION != null) {
            RunnerBootstrap.INSTRUMENTATION.appendToSystemClassLoaderSearch(new JarFile(file()));
        }

        RunnerBootstrap.LOADER.addURL(this.file.toURI().toURL());
    }

    public boolean download(String url) {
        try (var inputStream = new URL(url).openStream();
             var outputStream = new BufferedOutputStream(new FileOutputStream(file.toString()))) {

            var buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

}