package de.polocloud.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The subset of GitHub's release object (`GET /repos/{owner}/{repo}/releases`) that
 * [UpdateChecker] actually needs. `@Serializable` with `ignoreUnknownKeys` so the rest of
 * GitHub's (much larger) payload is simply skipped rather than failing decoding.
 *
 * Deliberately excludes GitHub's `prerelease` flag: PoloCloud's own stability model lives
 * in the tag ([tagName], parsed into a [de.polocloud.common.version.PolocloudVersion] with
 * its channel) and [UpdateChecker] compares against that, not against GitHub's metadata —
 * see [UpdateChecker] for why the two aren't interchangeable.
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
)