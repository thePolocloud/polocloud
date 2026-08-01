package de.polocloud.addon

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Resolves the well-known `%...%` placeholders supported inside a [de.polocloud.addon.display.MobDisplay]'s
 * hologram lines and inventory item text — mirrors [de.polocloud.addons.sign.system.layout.SignPlaceholders],
 * split into two scopes since a server mob's hologram describes a whole group while its inventory
 * items each describe one specific [Service]:
 *
 * - [group] resolves placeholders that aggregate across every service of the mob's group (the
 *   hologram, and [de.polocloud.addon.messages.Messages]' `%group%` usages).
 * - [service] resolves placeholders scoped to a single service (each inventory item).
 */
object ServerMobPlaceholders {

    /** Aggregates across [services] — used for the hologram and for messages. */
    fun group(text: String, group: String, services: List<Service> = emptyList()): String =
        text
            .replace("%group%", group)
            .replace("%players%", services.sumOf { it.onlinePlayers }.toString())
            .replace("%maxplayers%", services.sumOf { it.maxPlayers }.toString())
            .replace("%onlineservices%", services.count { it.state == ServiceState.RUNNING }.toString())
            .replace("%services%", services.size.toString())

    /** Scoped to a single [service] — used for inventory item names/lore. */
    fun service(text: String, group: String, service: Service): String =
        text
            .replace("%group%", group)
            .replace("%service%", service.name())
            .replace("%state%", service.state.toString())
            .replace("%online%", service.onlinePlayers.toString())
            .replace("%max%", service.maxPlayers.toString())
            .replace("%memory%", service.usedMemory.toInt().toString())
            .replace("%cpu%", "%.1f".format(service.cpuUsage))
            .replace("%host%", service.host)
            .replace("%port%", service.port.toString())
}
