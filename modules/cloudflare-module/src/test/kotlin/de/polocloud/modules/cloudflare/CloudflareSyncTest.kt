package de.polocloud.modules.cloudflare

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CloudflareSyncTest {

    private fun record(comment: String, id: String = comment) =
        CloudflareRecord(id = id, name = "play.example.com", content = "1.2.3.4", comment = comment)

    @Test
    fun `a freshly online proxy with no existing record is queued for creation`() {
        val managed = emptyList<CloudflareRecord>()
        val desired = mapOf("polocloud:lobby-1" to "10.0.0.1")

        val toCreate = CloudflareSync.entriesToCreate(managed, desired)

        assertEquals(mapOf("polocloud:lobby-1" to "10.0.0.1"), toCreate)
    }

    @Test
    fun `an already-managed proxy is not queued for creation again`() {
        val managed = listOf(record("polocloud:lobby-1"))
        val desired = mapOf("polocloud:lobby-1" to "10.0.0.1")

        val toCreate = CloudflareSync.entriesToCreate(managed, desired)

        assertTrue(toCreate.isEmpty())
    }

    @Test
    fun `a managed record for a proxy that's no longer online is queued for deletion`() {
        val managed = listOf(record("polocloud:lobby-1"), record("polocloud:lobby-2"))
        val desiredComments = setOf("polocloud:lobby-1")

        val toDelete = CloudflareSync.recordsToDelete(managed, desiredComments)

        assertEquals(listOf(record("polocloud:lobby-2")), toDelete)
    }

    @Test
    fun `a still-online proxy's record is never queued for deletion`() {
        val managed = listOf(record("polocloud:lobby-1"))
        val desiredComments = setOf("polocloud:lobby-1")

        val toDelete = CloudflareSync.recordsToDelete(managed, desiredComments)

        assertTrue(toDelete.isEmpty())
    }

    @Test
    fun `a reconcile pass creates missing entries and deletes stale ones in the same call`() {
        val managed = listOf(record("polocloud:lobby-1"), record("polocloud:lobby-stale"))
        val desired = mapOf("polocloud:lobby-1" to "10.0.0.1", "polocloud:lobby-2" to "10.0.0.2")

        val toCreate = CloudflareSync.entriesToCreate(managed, desired)
        val toDelete = CloudflareSync.recordsToDelete(managed, desired.keys)

        assertEquals(mapOf("polocloud:lobby-2" to "10.0.0.2"), toCreate)
        assertEquals(listOf(record("polocloud:lobby-stale")), toDelete)
    }
}
