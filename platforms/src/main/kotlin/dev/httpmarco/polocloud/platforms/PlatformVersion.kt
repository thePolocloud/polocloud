package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable

@Serializable
class PlatformVersion(private val version: String, private val buildId: String) {
}