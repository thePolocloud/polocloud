package de.polocloud.addon.config

import de.polocloud.common.configuration.SingleDocumentStorage

/**
 * Holds the current, in-memory value of a [SingleDocumentStorage]-backed document, reloading
 * it whenever the backing file changes on disk — mirrors how [de.polocloud.addons.sign.system.layout.LayoutRegistry]
 * wraps [de.polocloud.addons.sign.system.layout.LayoutStorage] for the sign-system addon, just
 * generic enough to cover a single document instead of a list of layouts.
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

    fun stop() = storage.stopWatching()
}
