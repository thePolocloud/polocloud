package de.polocloud.shared.event.terminal

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Fired by a node asking whichever service/plugin is registered under [serviceName] to
 * suggest tab completions for [buffer], the console input typed so far in a
 * `service <name> screen` session.
 *
 * Broadcast cluster-wide like every other [Event] — only the addressed service is expected
 * to answer, with a [TabCompleteResponseEvent] carrying the same [requestId]. Support is
 * opt-in per platform: a service that never subscribes to this event (or ignores requests
 * not addressed to it) simply never answers, and the asking node treats that as "no
 * completions available" once its wait times out.
 *
 * @param requestId correlates this request with the matching [TabCompleteResponseEvent].
 * @param serviceName the service this request is addressed to; every other subscriber must ignore it.
 * @param buffer the console input typed so far.
 */
@Serializable
data class TabCompleteRequestEvent(
    val requestId: String,
    val serviceName: String,
    val buffer: String,
) : Event