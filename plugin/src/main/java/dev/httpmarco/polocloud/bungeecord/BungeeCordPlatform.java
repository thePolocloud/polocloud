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

package dev.httpmarco.polocloud.bungeecord;

import dev.httpmarco.polocloud.RunningPlatform;
import dev.httpmarco.polocloud.RunningProxyPlatform;
import dev.httpmarco.polocloud.bungeecord.command.CloudCommand;
import dev.httpmarco.polocloud.bungeecord.listener.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;

public final class BungeeCordPlatform extends Plugin {

    private RunningPlatform runningPlatform;

    @Override
    public void onEnable() {
        var instance = ProxyServer.getInstance();
        instance.getConfigurationAdapter().getServers().clear();
        instance.getServers().clear();

        var pluginManager = instance.getPluginManager();
        pluginManager.registerListener(this, new PlayerDisconnectListener());
        pluginManager.registerListener(this, new PreLoginListener());
        pluginManager.registerListener(this, new ServerConnectedListener());
        pluginManager.registerListener(this, new PlayerLoginListener());

        pluginManager.registerCommand(this, new CloudCommand());

        instance.setReconnectHandler(new BungeeCordReconnectHandler());

        this.runningPlatform = new RunningProxyPlatform(
                it -> registerServer(it.name(), it.hostname(), it.port()),
                it -> ProxyServer.getInstance().getServers().remove(it.name()));

        this.runningPlatform.changeToOnline();
    }

    private void registerServer(String name, String hostname, int port) {
        var info = ProxyServer.getInstance().constructServerInfo(name, new InetSocketAddress(hostname, port), "PoloCloud Service", false);
        ProxyServer.getInstance().getServers().put(name, info);
    }
}
