package dev.httpmarco.polocloud.proxy.platform.bungeecord.listener;

import dev.httpmarco.polocloud.proxy.platform.bungeecord.BungeeCordPlatformPlugin;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@AllArgsConstructor
public class ServerSwitchListener implements Listener {

    private final BungeeCordPlatformPlugin platform;

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        this.platform.getBungeeTablistHandler().addPlayer(event.getPlayer());
    }
}
