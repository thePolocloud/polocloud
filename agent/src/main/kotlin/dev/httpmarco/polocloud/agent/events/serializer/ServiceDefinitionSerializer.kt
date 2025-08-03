package dev.httpmarco.polocloud.agent.events.serializer

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import dev.httpmarco.polocloud.agent.services.Service
import java.lang.reflect.Type

class ServiceDefinitionSerializer : JsonSerializer<Service> {

    override fun serialize(
        src: Service,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        val values = JsonObject()
        values.addProperty("groupName", src.group.data.name)
        values.addProperty("hostname", src.hostname)
        values.addProperty("id", src.id)
        values.addProperty("port", src.port)
        values.addProperty("type", src.group.platform().type.name)
        values.add("properties", context.serialize(src.properties))
        return values
    }
}