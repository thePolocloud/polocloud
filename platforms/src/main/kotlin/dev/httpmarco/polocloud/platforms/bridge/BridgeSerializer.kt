package dev.httpmarco.polocloud.platforms.bridge

import dev.httpmarco.polocloud.platforms.PlatformPool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class BridgeSerializer : KSerializer<Bridge?> {

    override val descriptor = PrimitiveSerialDescriptor("bridge", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Bridge? {
        return PlatformPool.findBindBridge(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Bridge?) {
        if (value != null) {
            encoder.encodeString(value.name)
        }
    }
}