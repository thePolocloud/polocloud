package dev.httpmarco.polocloud.platforms.bridge

import java.nio.file.Path
import kotlin.io.path.exists

data class Bridge(val id: String, val version: String) {

    @Transient
    lateinit var path: Path

    fun isDownloaded() : Boolean {
        return this::path.isInitialized && path.exists()
    }

    fun download() : Boolean {
        return true
    }

    fun type() : BridgeType {
        return BridgeType.ON_PREMISE
    }

    fun bridgeClass() : String {
        return ""
    }
}