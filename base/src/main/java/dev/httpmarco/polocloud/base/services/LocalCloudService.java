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

package dev.httpmarco.polocloud.base.services;

import dev.httpmarco.osgan.networking.channel.ChannelTransmit;
import dev.httpmarco.polocloud.api.groups.CloudGroup;
import dev.httpmarco.polocloud.api.groups.GroupProperties;
import dev.httpmarco.polocloud.api.packets.general.OperationDoublePacket;
import dev.httpmarco.polocloud.api.services.ServiceState;
import dev.httpmarco.polocloud.base.Node;
import dev.httpmarco.pololcoud.common.files.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Accessors(fluent = true)
public final class LocalCloudService extends CloudServiceImpl {

    private final Path runningFolder;
    @Setter
    private Process process;

    @Setter
    private ChannelTransmit channelTransmit;

    public LocalCloudService(CloudGroup group, int orderedId, UUID id, int port, ServiceState state) {
        super(group,
            orderedId,
                id,
                port,
                group.platform().proxy() ? "0.0.0.0" : Node.instance().nodeProvider().localEndpoint().data().hostname(),
                state,
                group.memory(),
                group.properties().has(GroupProperties.MAX_PLAYERS) ? group.properties().property(GroupProperties.MAX_PLAYERS) : 100,
                Node.instance().nodeProvider().localEndpoint());

        if (group.properties().has(GroupProperties.STATIC)) {
            this.runningFolder = FileUtils.createDirectory("static/" + name());
        } else {
            this.runningFolder = FileUtils.createDirectory("running/" + name() + "-" + id());
        }
    }

    @Override
    public CompletableFuture<Double> currentMemoryAsync() {
        var future = new CompletableFuture<Double>();
        channelTransmit.request("service-memory", OperationDoublePacket.class, operationDoublePacket -> future.complete(operationDoublePacket.response()));
        return future;
    }

    @SneakyThrows
    public void execute(String execute) {
        if (this.process == null) {
            return;
        }
        var writer = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream()));
        writer.write(execute);
        writer.newLine();
        writer.flush();
    }

    @Override
    @SneakyThrows
    public List<String> log() {
        var logs = new ArrayList<String>();
        var inputStream = this.process.getInputStream();
        var bytes = new byte[2048];
        int length;
        while (inputStream.available() > 0 && (length = inputStream.read(bytes, 0, bytes.length)) != -1) {
            logs.addAll(Arrays.stream(new String(bytes, 0, length, StandardCharsets.UTF_8).split("\n")).toList());
        }
        return logs;
    }
}
