package dev.httpmarco.polocloud.updater

import com.google.gson.reflect.TypeToken
import dev.httpmarco.polocloud.common.PolocloudUrl
import dev.httpmarco.polocloud.common.json.GSON
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val GITHUB_API_URL = PolocloudUrl.GITHUB_API + "polocloud/tags"

/**
 * Fetches a list of tag names from the GitHub API for the PoloCloud repository.
 *
 * @return List of tag names, or an empty list if the request fails.
 */
fun readTags(): List<String> {
    val client = HttpClient.newHttpClient()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(GITHUB_API_URL))
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "Mozilla/5.0")
        .GET()
        .build()

    return try {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 200) {
            // Deserialize JSON into a list of GitHubTag
            val type = object : TypeToken<List<GitHubTag>>() {}.type
            val tags = GSON.fromJson<List<GitHubTag>>(response.body(), type)

            // Extract names from tags
            tags.map { it.name }
        } else {
            System.err.println("Failed to fetch tags: HTTP ${response.statusCode()}")
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}