package de.polocloud.addon.bukkit

import de.polocloud.addon.ServerMobAddon
import de.polocloud.addon.bukkit.commands.BukkitServerMobsCommand
import de.polocloud.addon.config.ReloadableConfig
import de.polocloud.addon.display.MobDisplay
import de.polocloud.addon.messages.Messages
import de.polocloud.common.configuration.SingleDocumentStorage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitServerMobsBootstrap : JavaPlugin() {

    private lateinit var platform: BukkitServerMobPlatform
    private lateinit var addon: ServerMobAddon
    private lateinit var messages: ReloadableConfig<Messages>
    private lateinit var display: ReloadableConfig<MobDisplay>

    override fun onEnable() {
        messages = ReloadableConfig(SingleDocumentStorage(dataFolder.toPath().resolve("messages.json"), Messages.serializer())) { Messages() }
        display = ReloadableConfig(SingleDocumentStorage(dataFolder.toPath().resolve("display.json"), MobDisplay.serializer())) { MobDisplay() }

        platform = BukkitServerMobPlatform(this, display)
        addon = ServerMobAddon(platform)
        addon.start()
        platform.startLookTask()
        platform.startSpinTask()

        val command = BukkitServerMobsCommand(addon, platform, messages)
        getCommand("servermobs")?.setExecutor(command)
        getCommand("servermobs")?.tabCompleter = command
        Bukkit.getPluginManager().registerEvents(BukkitListener(addon, platform, display), this)
    }

    override fun onDisable() {
        platform.stopLookTask()
        platform.stopSpinTask()
        addon.stop()
        // Not part of ServerMobAddon's platform-agnostic contract — armor stands are a
        // Bukkit-only implementation detail of BukkitMobHologram, so their cleanup belongs
        // here, mirroring the sign-system addon's BukkitBootstrap.
        platform.clearHolograms()

        messages.stop()
        display.stop()
    }
}
