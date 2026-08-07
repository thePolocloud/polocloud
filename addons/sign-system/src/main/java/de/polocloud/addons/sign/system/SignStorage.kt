package de.polocloud.addons.sign.system

import de.polocloud.common.configuration.SingleDocumentStorage
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Persists attached [SignEntry]s (minus their live [de.polocloud.shared.service.Service]
 * binding, which is re-resolved on the next [SignSystem.start]) to [file] as JSON.
 *
 * Without this, every attached sign would need to be re-added by hand on every
 * restart — defeating the point of showing already-running servers immediately on
 * boot. Built on the shared [SingleDocumentStorage] (also used by [de.polocloud.addons.sign.system.layout.LayoutStorage]
 * and the proxy-addon/server-mobs-addon configs) rather than a bespoke JSON read/write.
 */
class SignStorage(file: Path) {

    @Serializable
    private data class Entry(
        val type: String,
        val group: String,
        val layoutId: String,
        val position: SignPosition,
    )

    @Serializable
    private data class Document(val entries: List<Entry> = emptyList())

    private val storage = SingleDocumentStorage(file, Document.serializer())

    fun load(): List<SignEntry> {
        val document = storage.readOrNull() ?: Document()

        return document.entries.map { entry ->
            SignEntry(SignEntryType(entry.type), entry.position, entry.group, entry.layoutId)
        }
    }

    fun save(entries: Collection<SignEntry>) {
        storage.save(Document(entries.map { Entry(it.type.id, it.group, it.layoutId, it.position) }))
    }
}