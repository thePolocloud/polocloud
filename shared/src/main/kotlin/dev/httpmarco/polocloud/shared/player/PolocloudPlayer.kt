package dev.httpmarco.polocloud.shared.player

import com.google.gson.JsonObject
import dev.httpmarco.polocloud.shared.service.Service
import dev.httpmarco.polocloud.v1.player.PlayerSnapshot
import java.util.UUID

open class PolocloudPlayer(
    val name: String,
    val uniqueId: UUID,
    val currentServiceName: String,
) {

    fun uniqueId(): String = uniqueId.toString()

    companion object {
        fun bindSnapshot(snapshot: PlayerSnapshot): PolocloudPlayer {
            return PolocloudPlayer(
                name = snapshot.name,
                uniqueId = UUID.fromString(snapshot.uniqueId),
                currentServiceName = snapshot.currentServiceName
            )
        }
    }

    fun toSnapshot(): PlayerSnapshot {
        return PlayerSnapshot.newBuilder()
            .setName(name)
            .setUniqueId(uniqueId.toString())
            .setCurrentServiceName(currentServiceName)
            .build()
    }

    fun message(message: String) {

    }

    fun kick(message: String) {

    }

    fun toServer(serviceName: String) {

    }

    fun toServer(service: Service) {
        this.toServer(service.name())
    }
}

fun PolocloudPlayer.toJson(): JsonObject {
    return JsonObject().apply {
        addProperty("name", name)
        addProperty("uniqueId", uniqueId.toString())
        addProperty("currentServiceName", currentServiceName)
    }
}
