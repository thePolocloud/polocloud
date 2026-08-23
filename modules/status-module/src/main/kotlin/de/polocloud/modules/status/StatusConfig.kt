package de.polocloud.modules.status

/**
 * `config.yml` for the status module (see [de.polocloud.moduleapi.ModuleNode.config]).
 */
data class StatusConfig(
    /** Interface the REST API (and, if [hostWebsite] is on, the website) binds to. */
    var host: String = "0.0.0.0",
    var port: Int = 7020,
    /**
     * Whether to serve the status website bundled into this module's own jar
     * (`src/main/resources/webroot/`) alongside the REST API. Turn off to expose only
     * `/api/status` and host a separate frontend elsewhere against it.
     */
    var hostWebsite: Boolean = true,
    /** Sent as `Access-Control-Allow-Origin: *` on API responses, so a status page hosted elsewhere can call this API directly. */
    var corsEnabled: Boolean = true,
    /**
     * Which groups appear on the status page, keyed by group name — a group that
     * exists in the cloud but isn't listed here simply doesn't show up. Add or remove
     * entries by hand to change what's published.
     */
    var groups: MutableMap<String, StatusGroupConfig> = linkedMapOf(
        "lobby" to StatusGroupConfig(displayName = "Lobby"),
        "bedwars" to StatusGroupConfig(displayName = "BedWars"),
    ),
)

data class StatusGroupConfig(
    var displayName: String = "",
    /**
     * Manual availability switch, independent of whether a service of this group is
     * actually running right now — set to `false` to show this group as "in
     * maintenance" ahead of time (e.g. before taking bedwars offline for an update).
     */
    var available: Boolean = true,
)
