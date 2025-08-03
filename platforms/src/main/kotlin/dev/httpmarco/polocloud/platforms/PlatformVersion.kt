package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = PlatformVersionSerializer::class)
data class PlatformVersion(val version: String, val additionalProperties: Map<String, JsonElement> = emptyMap()) {
    val buildId: String?
        get() = additionalProperties["buildId"]?.jsonPrimitive?.content

    val requiredRuntimeVersion: String?
        get() = additionalProperties["requiredRuntimeVersion"]?.jsonPrimitive?.content

    fun getProperty(key: String): JsonElement? = additionalProperties[key]

    fun getStringProperty(key: String): String? = additionalProperties[key]?.jsonPrimitive?.content

    fun getBooleanProperty(key: String): Boolean? =
        additionalProperties[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull()

    fun getIntProperty(key: String): Int? = additionalProperties[key]?.jsonPrimitive?.content?.toIntOrNull()
}