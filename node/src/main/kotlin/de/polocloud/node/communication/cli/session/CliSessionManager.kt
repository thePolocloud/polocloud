package de.polocloud.node.communication.cli.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CliSessionManager : ICliSessionManager {

    // Keyed by the lowercased subject (a cert CN) — applied consistently at every read
    // and write site below — so lookup/removal can't silently miss (and leak a session)
    // just because a caller's casing differs from the one a session was created with.
    private val sessions = ConcurrentHashMap<String, CliSession>()

    override fun createOrUpdate(subject: String, address: String): CliSession {
        val now = now()
        return sessions.compute(subject.lowercase()) { _, existing ->
            existing?.copy(address = address, lastAccess = now)
                ?: CliSession(
                    sessionId  = UUID.randomUUID().toString(),
                    subject    = subject,
                    address    = address,
                    connectedAt = now,
                    lastAccess = now,
                )
        }!!
    }

    override fun touch(subject: String) {
        val now = now()
        sessions.computeIfPresent(subject.lowercase()) { _, session -> session.copy(lastAccess = now) }
    }

    override fun remove(subject: String) {
        sessions.remove(subject.lowercase())
    }

    override fun get(subject: String): CliSession? = sessions[subject.lowercase()]

    override fun all(): Collection<CliSession> = sessions.values.toList()

    override fun findExpired(timeout: Long): List<CliSession> {
        val now = now()
        return sessions.values.filter { it.isExpired(timeout, now) }
    }

    override fun cleanupExpired(timeout: Long) {
        val now = now()
        sessions.entries.removeIf { it.value.isExpired(timeout, now) }
    }

    private fun now(): Long = System.currentTimeMillis()
}