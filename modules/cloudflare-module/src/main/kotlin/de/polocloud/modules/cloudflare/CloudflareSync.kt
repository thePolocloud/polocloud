package de.polocloud.modules.cloudflare

internal object CloudflareSync {

    /** Managed records whose comment no longer corresponds to an online proxy. */
    fun recordsToDelete(managed: List<CloudflareRecord>, desiredComments: Set<String>): List<CloudflareRecord> =
        managed.filter { it.comment !in desiredComments }

    /** Desired (comment -> host) entries that don't have a managed record yet. */
    fun entriesToCreate(managed: List<CloudflareRecord>, desired: Map<String, String>): Map<String, String> {
        val existingComments = managed.mapNotNull { it.comment }.toSet()
        return desired.filterKeys { it !in existingComments }
    }
}
