package de.polocloud.modules.cloudflare

data class CloudflareConfig(
    /** Cloudflare API token with `Zone:DNS:Edit` permission on [zoneId]. */
    var apiToken: String = "",
    /** The zone (domain) id the DNS record lives in. */
    var zoneId: String = "",
    /** The hostname to keep pointed at every online proxy, e.g. `play.example.com`. */
    var record: String = "",
    var ttl: Int = 60,
    /** Whether the record should be proxied through Cloudflare's network (orange cloud) — usually `false` for a raw Minecraft TCP endpoint. */
    var proxied: Boolean = false,
    /** How often to reconcile as a drift-correction safety net, on top of reacting to service events immediately. */
    var reconcileIntervalSeconds: Long = 60,
)
