package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
data class PlatformIndex(val group: String, val version: String) {

}