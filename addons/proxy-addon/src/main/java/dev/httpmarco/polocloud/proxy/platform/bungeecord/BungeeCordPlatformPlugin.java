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

package dev.httpmarco.polocloud.proxy.platform.bungeecord;

import dev.httpmarco.polocloud.proxy.config.Config;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.command.ProxyCommand;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.listener.PingListener;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.listener.PlayerDisconnectListener;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.listener.PostLoginListener;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.configuration.maintenance.BungeeMaintenanceProvider;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.configuration.tablist.BungeeTablistHandler;
import dev.httpmarco.polocloud.proxy.platform.bungeecord.listener.ServerSwitchListener;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class BungeeCordPlatformPlugin extends Plugin {

    private final Config config = new Config();
    private BungeeTablistHandler bungeeTablistHandler;
    private BungeeMaintenanceProvider bungeeMaintenanceProvider;

    @Override
    public void onEnable() {
        this.bungeeTablistHandler = new BungeeTablistHandler(this);
        this.bungeeMaintenanceProvider = new BungeeMaintenanceProvider();

        var instance = ProxyServer.getInstance();

        var pluginManager = instance.getPluginManager();
        pluginManager.registerListener(this, new PingListener(this));
        pluginManager.registerListener(this, new PostLoginListener(this));
        pluginManager.registerListener(this, new PlayerDisconnectListener(this));
        pluginManager.registerListener(this, new ServerSwitchListener(this));

        pluginManager.registerCommand(this, new ProxyCommand());
    }
}
