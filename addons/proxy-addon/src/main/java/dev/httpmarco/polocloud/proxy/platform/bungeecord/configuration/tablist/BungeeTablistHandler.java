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

package dev.httpmarco.polocloud.proxy.platform.bungeecord.configuration.tablist;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.BungeeCordPlatformPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeTablistHandler extends BungeeTablistProvider {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public BungeeTablistHandler(BungeeCordPlatformPlugin platform) {
        super(platform);
    }

    public void addPlayer(ProxiedPlayer player) {
        update(player);
    }

    public void update(ProxiedPlayer player) {
        var cloudPlayer = CloudAPI.instance().playerProvider().find(player.getUniqueId());

        player.sendMessage(new TextComponent("current servername -> " + cloudPlayer.currentServerName()));

        var header = formatPlaceholders(getTablist().header(), cloudPlayer);
        var footer = formatPlaceholders(getTablist().footer(), cloudPlayer);

        player.setTabHeader(
                BungeeComponentSerializer.get().serialize(this.miniMessage.deserialize(header)),
                BungeeComponentSerializer.get().serialize(this.miniMessage.deserialize(footer)));
    }
}
