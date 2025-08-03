package dev.httpmarco.polocloud.platforms.tasks

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class PlatformTaskSerializer : KSerializer<PlatformTask> {

    override val descriptor = PrimitiveSerialDescriptor("task", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PlatformTask {
        return PlatformTaskPool.find(decoder.decodeString())
            ?: throw IllegalArgumentException("Unknown task type: ${decoder.decodeString()}")
    }

    override fun serialize(
        encoder: Encoder,
        value: PlatformTask
    ) {
        encoder.encodeString(value.name)
    }
}