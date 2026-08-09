package de.polocloud.modules.cloudflare

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class CloudflareClient(
    private val apiToken: String,
    private val zoneId: String,
) {

    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records"

    /** All A records currently named [name] in this zone (Cloudflare allows several — one per online proxy). */
    fun listRecords(name: String): List<CloudflareRecord> {
        val request = requestBuilder("$baseUrl?type=A&name=$name&per_page=100").GET().build()
        val parsed = json.decodeFromString<CloudflareListResponse>(send(request))
        if (!parsed.success) throw CloudflareApiException(parsed.errors)
        return parsed.result
    }

    fun createRecord(record: CloudflareRecord): CloudflareRecord {
        val body = json.encodeToString(CloudflareRecord.serializer(), record)
        val request = requestBuilder(baseUrl).POST(HttpRequest.BodyPublishers.ofString(body)).build()
        val parsed = json.decodeFromString<CloudflareRecordResponse>(send(request))
        if (!parsed.success || parsed.result == null) throw CloudflareApiException(parsed.errors)
        return parsed.result
    }

    /** No-op if [id] is already gone — deleting an already-deleted record isn't an error for our purposes. */
    fun deleteRecord(id: String) {
        val request = requestBuilder("$baseUrl/$id").DELETE().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299 && response.statusCode() != 404) {
            error("Cloudflare API returned HTTP ${response.statusCode()} deleting record '$id': ${response.body()}")
        }
    }

    private fun requestBuilder(url: String): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer $apiToken")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))

    private fun send(request: HttpRequest): String {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Cloudflare API returned HTTP ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }
}
