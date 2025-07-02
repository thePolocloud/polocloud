package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
data class Platform(
    val name: String,
    val type: PlatformType,
    val language: PlatformLanguage,
    val url: String,
    val versions: List<PlatformVersion>
) {

    fun prepare(version: String) {
        TODO()
    }
}