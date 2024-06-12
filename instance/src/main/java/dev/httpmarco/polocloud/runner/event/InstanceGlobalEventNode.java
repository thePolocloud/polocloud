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

package dev.httpmarco.polocloud.runner.event;

import dev.httpmarco.polocloud.api.events.Event;
import dev.httpmarco.polocloud.api.events.EventNode;
import dev.httpmarco.polocloud.api.events.EventRunnable;
import dev.httpmarco.polocloud.api.packets.event.CloudEventCallPacket;
import dev.httpmarco.polocloud.api.packets.event.CloudEventRegitserPacket;
import dev.httpmarco.polocloud.runner.CloudInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class InstanceGlobalEventNode implements EventNode {

    private final HashMap<Class<? extends Event>, List<EventRunnable<? extends Event>>> nodeListeners = new HashMap<>();

    public InstanceGlobalEventNode() {
        CloudInstance.instance().client().transmitter().listen(CloudEventCallPacket.class, (channelTransmit, event) -> {
            if (nodeListeners.containsKey(event.event().getClass())) {
                for (EventRunnable<? extends Event> runnable : nodeListeners.get(event.event().getClass())) {
                    runnable.runMapped(event.event());
                }
            }
        });
    }

    @Override
    public <T extends Event> void addListener(Class<T> event, EventRunnable<T> runnable) {
        var currentListeners = nodeListeners.getOrDefault(event, new ArrayList<>());
        currentListeners.add(runnable);
        this.nodeListeners.put(event, currentListeners);

        CloudInstance.instance().client().transmitter().sendPacket(new CloudEventRegitserPacket(CloudInstance.SELF_ID, event.getName()));
    }

    @Override
    public void call(Event event) {

    }
}
