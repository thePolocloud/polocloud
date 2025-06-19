package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

private val STORAGE_PATH = Path("local/groups")

class LocalRuntimeGroupStorage : RuntimeGroupStorage {

    init {
        STORAGE_PATH.toFile().mkdirs()
    }

    override fun items(): List<Group> {
        return STORAGE_PATH.listDirectoryEntries("*.json").stream().map {
            return@map Group(Json.decodeFromString(Files.readString(it)))
        }.toList()
    }

    override fun item(identifier: String): Group? {
        TODO("Not yet implemented")
    }
}