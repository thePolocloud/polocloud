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

package dev.httpmarco.polocloud.base.groups.platforms;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.api.groups.platforms.PlatformVersion;
import dev.httpmarco.pololcoud.common.GsonUtils;
import dev.httpmarco.pololcoud.common.StringUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public abstract class PaperMCPlatform extends Platform {

    private static final String VERSION_URL = "https://api.papermc.io/v2/projects/%s";
    private static final String BUILD_URL = "https://api.papermc.io/v2/projects/%s/versions/%s/builds";
    private static final String DOWNLOAD_URL = "https://api.papermc.io/v2/projects/%s/versions/%s/builds/%d/downloads/%s-%s.jar";

    private final String product;

    public PaperMCPlatform(String product, boolean proxy) {
        super(proxy);
        this.product = product;
        for (var version : readPaperInformation(VERSION_URL.formatted(product)).get("versions").getAsJsonArray()) {
            possibleVersions().add(new PlatformVersion(product + "-" + version.getAsString(), proxy));
        }
    }

    @SneakyThrows
    private JsonObject readPaperInformation(String link) {
        return GsonUtils.GSON.fromJson(StringUtils.downloadStringContext(link), JsonObject.class);
    }

    @Override
    public String[] platformsArguments() {
        return new String[]{"nogui", "noconsole"};
    }

    @Override
    public String[] platformsEnvironment() {
        return new String[]{};
    }

    @Override
    @SneakyThrows
    public void download(String version) {
        var orgVersion = version.replace(product + "-", "");

        // search for the current build version
        var builds = GsonUtils.GSON.fromJson(StringUtils.downloadStringContext(BUILD_URL.formatted(product, orgVersion)), JsonObject.class).get("builds").getAsJsonArray().asList();
        var buildIndex = builds.get(builds.size() - 1).getAsJsonObject().get("build").getAsInt();

        //todo duplicated code
        var platformPath = Path.of("local").resolve("platforms").resolve(version);
        final var url = URI.create(DOWNLOAD_URL.formatted(product, orgVersion, buildIndex, version, buildIndex)).toURL();
        platformPath.toFile().mkdirs();

        Files.copy(url.openConnection().getInputStream(), Path.of(platformPath + "/server.jar"));

        // pre build
        Files.createDirectory(platformPath.resolve("cache"));
        try {
            java.nio.file.Files.copy(platformPath.resolve("server.jar"), platformPath.resolve("cache").resolve("build.jar"));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        //todo better
        // check if papermcplatform is velocity
        if (version.startsWith("velocity")) {
            return;
        }

        try {
            var process = new ProcessBuilder("java", "-Dpaperclip.patchonly=true", "-jar", "build.jar").directory(platformPath.resolve("cache").toFile()).start();
            process.waitFor(2, TimeUnit.MINUTES);
            process.destroy();
            Files.delete(platformPath.resolve("cache").resolve("build.jar"));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
