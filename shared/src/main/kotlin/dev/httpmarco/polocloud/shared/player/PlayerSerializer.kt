package dev.httpmarco.polocloud.shared.player

import com.google.gson.*
import java.lang.reflect.Type
import java.util.UUID

class PlayerSerializer : JsonSerializer<PolocloudPlayer>, JsonDeserializer<PolocloudPlayer> {

    override fun serialize(
        src: PolocloudPlayer,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = JsonObject()

        json.addProperty("uniqueId", src.uniqueId())
        json.addProperty("name", src.name)
        json.addProperty("currentService", src.currentServiceName)

        return json
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PolocloudPlayer {
        val obj = json.asJsonObject

        val uniqueId = obj.get("uniqueId").asString
        val name = obj.get("name").asString
        val currentService = obj.get("currentService").asString
        val currentProxyName = obj.get("currentProxyName").asString

        return PolocloudPlayer(
            name = name,
            uniqueId = UUID.fromString(uniqueId),
            currentServiceName = currentService,
            currentProxyName = currentProxyName
        )
    }
}