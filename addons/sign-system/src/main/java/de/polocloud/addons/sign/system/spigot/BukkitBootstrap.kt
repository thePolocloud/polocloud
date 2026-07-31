package de.polocloud.addons.sign.system.spigot

import de.polocloud.addons.sign.system.SignSystem
import de.polocloud.addons.sign.system.spigot.command.SignCommand
import de.polocloud.addons.sign.system.spigot.renderer.hologram.BukkitHologram
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitBootstrap : JavaPlugin() {

    private lateinit var platform: BukkitSignPlatform
    private lateinit var signSystem: SignSystem

    override fun onEnable() {
        platform = BukkitSignPlatform(this)
        signSystem = SignSystem(platform)
        signSystem.start()

        getCommand("signs")?.setExecutor(SignCommand(signSystem, platform))

        Bukkit.getPluginManager().registerEvents(BukkitListener(this, signSystem), this)

        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
    }

    override fun onDisable() {
        signSystem.stop()
        // Not part of SignSystem's platform-agnostic contract — armor stands are a
        // Bukkit-only implementation detail of BukkitBannerRenderer's hologram, so their
        // cleanup belongs here rather than being threaded through SignSystem/SignPlatform.
        BukkitHologram.hideAll()

        this.server.messenger.unregisterOutgoingPluginChannel(this);
    }
}