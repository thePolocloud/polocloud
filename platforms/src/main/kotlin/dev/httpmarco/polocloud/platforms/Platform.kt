package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
class Platform(
    val name: String,
    val type: PlatformType,
    val language: PlatformLanguage,
    val url: String,
    val versions: List<PlatformVersion>
) {


}