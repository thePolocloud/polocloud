package de.polocloud.addon.bukkit

import de.polocloud.addon.ServerMobAddon
import de.polocloud.addon.bukkit.commands.BukkitServerMobsCommand
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitServerMobsBootstrap : JavaPlugin() {

    private lateinit var platform: BukkitServerMobPlatform
    private lateinit var addon: ServerMobAddon

    override fun onEnable() {
        platform = BukkitServerMobPlatform(this)
        addon = ServerMobAddon(platform)
        addon.start()

        getCommand("servermobs")?.setExecutor(BukkitServerMobsCommand(addon, platform))
        Bukkit.getPluginManager().registerEvents(BukkitListener(addon, platform), this)
    }

    override fun onDisable() {
        addon.stop()
    }
}
