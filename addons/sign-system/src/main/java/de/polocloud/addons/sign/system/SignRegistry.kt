package de.polocloud.addons.sign.system

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe collection of every [SignEntry] known to this platform instance.
 *
 * Backed by a [CopyOnWriteArrayList]: entries are attached/detached rarely (a player
 * running `/signs add`) but read constantly (every animation tick, every cluster
 * event), so optimising for cheap, lock-free reads is the right trade-off.
 */
class SignRegistry {

    private val entries = CopyOnWriteArrayList<SignEntry>()

    fun attach(entry: SignEntry) {
        entries += entry
    }

    fun detach(position: SignPosition): SignEntry? {
        val entry = at(position) ?: return null
        entries.remove(entry)
        return entry
    }

    fun all(): List<SignEntry> = entries

    /** The entry attached at [position], if any. */
    fun at(position: SignPosition): SignEntry? = entries.firstOrNull { it.position == position }

    /** A group's entry not yet bound to a service, if any — used to bind a newly started service. */
    fun findFree(group: String): SignEntry? =
        entries.firstOrNull { it.group.equals(group, ignoreCase = true) && it.service == null }

    /** Every entry currently bound to the service named [serviceName] (`group-index`). */
    fun boundTo(serviceName: String): List<SignEntry> =
        entries.filter { it.service?.name()?.equals(serviceName, ignoreCase = true) == true }
}