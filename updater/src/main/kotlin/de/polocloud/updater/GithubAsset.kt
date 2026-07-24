package de.polocloud.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single downloadable file attached to a [GithubRelease]. [Updater] looks for the
 * one ending in `.jar` — the launcher jar published by the release workflow (the
 * `runner` module's build output) — and downloads it over the currently running jar.
 */
@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)