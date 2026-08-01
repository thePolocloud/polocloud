package de.polocloud.addons.proxy

/** Resolves the well-known `%...%` placeholders supported inside tab list/MOTD lines. */
object ProxyPlaceholders {

    fun resolve(text: String, online: Int, max: Int, player: String = "", server: String = ""): String =
        text
            .replace("%online%", online.toString())
            .replace("%max%", max.toString())
            .replace("%player%", player)
            .replace("%server%", server)
}
