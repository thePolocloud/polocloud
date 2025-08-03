package dev.httpmarco.polocloud.platforms.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Path

@Serializable
data class Bridge(val name: String, val type: BridgeType) {

    @Transient
    lateinit var path: Path

}