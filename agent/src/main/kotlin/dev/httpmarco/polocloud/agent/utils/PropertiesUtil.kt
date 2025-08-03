package dev.httpmarco.polocloud.agent.utils
import kotlinx.serialization.json.*

fun Map<String, JsonElement>.asStringMap(): Map<String, String> {
    return this.mapValues { (_, value) ->
        when {
            value is JsonPrimitive && value.isString -> value.content
            value is JsonPrimitive -> value.toString()
            else -> Json.encodeToString(JsonElement.serializer(), value)
        }
    }
}