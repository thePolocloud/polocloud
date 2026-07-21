package de.polocloud.shared.event.terminal

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Answers a [TabCompleteRequestEvent] with [requestId], carrying the [suggestions] the
 * addressed service's own command handling would offer for that input.
 */
@Serializable
data class TabCompleteResponseEvent(
    val requestId: String,
    val suggestions: List<String>,
) : Event