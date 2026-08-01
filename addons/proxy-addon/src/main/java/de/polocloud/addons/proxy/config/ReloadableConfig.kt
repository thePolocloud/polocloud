package de.polocloud.addons.proxy.config

/**
 * Holds the current, in-memory value of a [SingleDocumentStorage]-backed document, reloading it
 * whenever the backing file changes on disk.
 */
class ReloadableConfig<T>(private val storage: SingleDocumentStorage<T>, private val default: () -> T) {

    @Volatile
    private var value: T = default()

    init {
        reload()
        storage.watch { reload() }
    }

    fun reload() {
        value = storage.load(default)
    }

    fun current(): T = value

    /**
     * Persists [newValue] and applies it in-memory immediately, for runtime toggles (e.g.
     * `/proxy maintenance`) that must not wait for the file watcher's own — functionally
     * idempotent, since it would just reload the exact same value — reload to notice.
     */
    fun update(newValue: T) {
        value = newValue
        storage.save(newValue)
    }

    fun stop() = storage.stopWatching()
}
