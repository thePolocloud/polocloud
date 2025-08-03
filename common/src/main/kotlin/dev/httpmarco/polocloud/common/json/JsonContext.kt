package dev.httpmarco.polocloud.common.json

import kotlinx.serialization.json.Json

val PRETTY_JSON = Json {
    prettyPrint = true
    classDiscriminator = "type"
}