package de.polocloud.modules.cloudflare

import kotlinx.serialization.Serializable

@Serializable
data class CloudflareRecord(
    val id: String? = null,
    val type: String = "A",
    val name: String,
    val content: String,
    val ttl: Int = 1,
    val proxied: Boolean = false,
    /** Used as this module's ownership marker — see [CloudflareModule]'s `MANAGED_PREFIX`. */
    val comment: String? = null,
)

@Serializable
data class CloudflareApiError(val code: Int = 0, val message: String = "")

@Serializable
internal data class CloudflareListResponse(
    val success: Boolean,
    val result: List<CloudflareRecord> = emptyList(),
    val errors: List<CloudflareApiError> = emptyList(),
)

@Serializable
internal data class CloudflareRecordResponse(
    val success: Boolean,
    val result: CloudflareRecord? = null,
    val errors: List<CloudflareApiError> = emptyList(),
)

/** Thrown when Cloudflare answers with `success: false`. */
class CloudflareApiException(errors: List<CloudflareApiError>) : IllegalStateException(
    errors.joinToString { "${it.code}: ${it.message}" }.ifBlank { "unknown Cloudflare API error" }
)
