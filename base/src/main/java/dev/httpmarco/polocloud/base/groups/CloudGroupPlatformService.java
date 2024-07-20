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

package dev.httpmarco.polocloud.base.groups;

import dev.httpmarco.polocloud.api.groups.platforms.PlatformVersion;
import dev.httpmarco.polocloud.base.groups.platforms.*;
import dev.httpmarco.polocloud.base.services.LocalCloudService;
import dev.httpmarco.polocloud.runner.RunnerBootstrap;
import dev.httpmarco.pololcoud.common.StringUtils;
import dev.httpmarco.pololcoud.common.files.FileUtils;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CloudGroupPlatformService {

    public static final String PROXY_SECRET = StringUtils.randomString(8);
    private static final Path PLATFORM_FOLDER = FileUtils.createDirectory("local/platforms");
    private final Set<Platform> platforms = Set.of(new PaperPlatform(), new VelocityPlatform(), new BungeeCordPlatform(), new MinestomPlatform(), new PurpurPlatform());

    public boolean isValidPlatform(String platform) {
        return platforms.stream().anyMatch(it -> it.possibleVersions().stream().anyMatch(v -> v.version().equalsIgnoreCase(platform)));
    }

    public Platform find(String platform) {
        return platforms.stream().filter(it -> it.possibleVersions().stream().anyMatch(v -> v.version().equalsIgnoreCase(platform))).findFirst().orElse(null);
    }

    private boolean isPlatformAvailableForRuntime(String platform) {
        return Files.exists(PLATFORM_FOLDER.resolve(platform).resolve("server.jar"));
    }

    @SneakyThrows
    public void preparePlatform(LocalCloudService cloudService) {
        String platformName = cloudService.group().platform().version();
        var platform = find(platformName);

        if (!isPlatformAvailableForRuntime(platformName)) {
            platform.download(platformName);
        }

        if (!(platform instanceof MinestomPlatform)) {
            var cacheDirectory = PLATFORM_FOLDER.resolve(platformName).resolve("cache");
            if (Files.exists(cacheDirectory)) {
                FileUtils.copyDirectoryContents(cacheDirectory, cloudService.runningFolder());
            }
            var platformSource = cloudService.runningFolder().resolve(platformName + ".jar");
            if (!Files.exists(platformSource)) {
                Files.copy(PLATFORM_FOLDER.resolve(platformName).resolve("server.jar"), platformSource);
            }
        }

        platform.prepare(cloudService);

        // append server icon
        var serverIconPath = cloudService.runningFolder().resolve("server-icon.png");
        if (!Files.exists(serverIconPath)) {
          Files.copy(Objects.requireNonNull(RunnerBootstrap.LOADER.getResourceAsStream("server-files/server-icon.png")), serverIconPath);
        }
    }


    public List<PlatformVersion> validPlatformVersions() {
        var versions = new ArrayList<PlatformVersion>();
        for (var platform : this.platforms) {
            versions.addAll(platform.possibleVersions());
        }
        return versions;
    }
}