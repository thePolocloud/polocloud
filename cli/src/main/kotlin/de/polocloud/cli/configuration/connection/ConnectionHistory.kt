package de.polocloud.cli.configuration.connection

import de.polocloud.cli.CliPaths
import kotlinx.serialization.json.Json
import java.security.KeyPair
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ConnectionHistory(
    private val keyPair: KeyPair,
    private val maxEntries: Int = 10
) {

    private val file = CliPaths.CACHE_DIR.resolve(".connection.history")
    private val json = Json { ignoreUnknownKeys = true }

    fun push(entry: ConnectionEntry) {
        val history = load().toMutableList()
        history.removeIf {
            it.clusterAddress == entry.clusterAddress
        }
        history.add(0, entry)
        if (history.size > maxEntries) {
            history.subList(maxEntries, history.size).clear()
        }
        save(history)
    }

    fun latest(): ConnectionEntry? = load().firstOrNull()

    fun all(): List<ConnectionEntry> = load()

    fun clear() { file.delete() }

    private fun deriveAesKey(): SecretKeySpec {
        val raw = keyPair.public.encoded.copyOf(32)
        return SecretKeySpec(raw, "AES")
    }

    private fun save(entries: List<ConnectionEntry>) {
        val plain = json.encodeToString(entries).toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain)

        val blob = Base64.getEncoder().encodeToString(iv + encrypted)
        file.writeText(blob)
    }

    private fun load(): List<ConnectionEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val blob = Base64.getDecoder().decode(file.readText().trim())
            val iv = blob.copyOf(12)
            val ciphertext = blob.copyOfRange(12, blob.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(ciphertext)

            json.decodeFromString<List<ConnectionEntry>>(String(plain))
        }.getOrElse {
            file.delete()
            emptyList()
        }
    }
}