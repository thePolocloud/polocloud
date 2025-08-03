package dev.httpmarco.polocloud.shared

import dev.httpmarco.polocloud.shared.events.EventProvider

lateinit var polocloudShared: PolocloudShared

abstract class PolocloudShared {

    abstract fun eventProvider(): EventProvider

    init {
        polocloudShared = this
    }
}