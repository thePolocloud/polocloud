package dev.httpmarco.polocloud.agent.player

import dev.httpmarco.polocloud.shared.player.PolocloudPlayer
import java.util.UUID

class AbstractPolocloudPlayer(
    name: String,
    uniqueId: UUID,
    currentServiceName: String,
    currentProxyName: String,
) : PolocloudPlayer(
    name,
    uniqueId,
    currentServiceName,
    currentProxyName
) {

}