package dev.httpmarco.polocloud.platforms

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


class PlatformVersionSerializer : KSerializer<PlatformVersion> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlatformVersion") {
        element<String>("version")
        element<JsonObject>("additionalProperties")
    }

    override fun serialize(encoder: Encoder, value: PlatformVersion) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.version)

        val jsonObject = JsonObject(
            mapOf("version" to JsonPrimitive(value.version)) + value.additionalProperties
        )
        composite.encodeSerializableElement(descriptor, 1, JsonObject.serializer(), jsonObject)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): PlatformVersion {
        val jsonDecoder = decoder as JsonDecoder
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        val version = jsonObject["version"]?.jsonPrimitive?.content
            ?: throw SerializationException("version field is required")

        val additionalProperties = jsonObject.filterKeys { it != "version" }

        return PlatformVersion(version, additionalProperties)
    }
}