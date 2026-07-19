package de.polocloud.updater

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Fetches a GitHub repository's release list. Kept behind an interface — mirrors
 * `PeerServiceQuery` elsewhere in the codebase — so [UpdateChecker]'s version-comparison
 * logic can be unit-tested without a real network call.
 */
fun interface ReleaseFetcher {

    fun fetchReleases(): List<GithubRelease>
}

/**
 * Real [ReleaseFetcher]: calls GitHub's public REST API. Releases come back newest-first,
 * which [UpdateChecker] relies on to find the latest non-draft one.
 */
class GithubReleaseFetcher(
    private val repository: String = "thePolocloud/polocloud",
    private val timeout: Duration = Duration.ofSeconds(5),
) : ReleaseFetcher {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    override fun fetchReleases(): List<GithubRelease> {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/$repository/releases"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "PoloCloud-UpdateChecker")
            .timeout(timeout)
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "GitHub releases request failed with status ${response.statusCode()}"
        }

        return json.decodeFromString(response.body())
    }
}