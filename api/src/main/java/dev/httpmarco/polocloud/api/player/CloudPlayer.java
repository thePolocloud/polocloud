package dev.httpmarco.polocloud.api.player;

import java.util.UUID;

public interface CloudPlayer {

    UUID uniqueID();

    String name();

    void sendMessage(String message);

    void sendActionBar(String message);

    void sendTitle(String title, String subtitle, Integer fadeIn, Integer stay, Integer fadeOut);

    void kick(String reason);

    String currentServer();

    String currentProxy();

    void connectToServer(String serverName);

}
