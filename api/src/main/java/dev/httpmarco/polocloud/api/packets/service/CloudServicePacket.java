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

package dev.httpmarco.polocloud.api.packets.service;

import dev.httpmarco.osgan.networking.packet.Packet;
import dev.httpmarco.osgan.networking.packet.PacketBuffer;
import dev.httpmarco.polocloud.api.packets.CloudComponentPacketHelper;
import dev.httpmarco.polocloud.api.services.CloudService;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public final class CloudServicePacket extends Packet {

    private CloudService service;

    public CloudServicePacket(CloudService service) {
        this.service = service;
    }

    @Override
    public void read(PacketBuffer codecBuffer) {
        this.service = CloudComponentPacketHelper.readService(codecBuffer);
    }

    @Override
    public void write(PacketBuffer codecBuffer) {
        CloudComponentPacketHelper.writeService(service, codecBuffer);
    }
}
