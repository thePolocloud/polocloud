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

package dev.httpmarco.polocloud.proxy.platform.bungeecord.listener;

import dev.httpmarco.polocloud.proxy.platform.bungeecord.BungeeCordPlatformPlugin;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@AllArgsConstructor
public class PlayerDisconnectListener implements Listener {

    private final BungeeCordPlatformPlugin platform;

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        this.platform.getProxy().getPlayers().stream().filter(player -> player != event.getPlayer()).forEach(player -> this.platform.getBungeeTablistHandler().update(player));
    }
}
