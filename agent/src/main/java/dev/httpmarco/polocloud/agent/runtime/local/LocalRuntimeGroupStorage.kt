package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

private val STORAGE_PATH = Path("local/groups")

class LocalRuntimeGroupStorage : RuntimeGroupStorage {

    private var cachedGroups = listOf<Group>()

    init {
        // create directory if it does not exist
        STORAGE_PATH.createDirectories()

        // load all groups from the storage path
        cachedGroups = STORAGE_PATH.listDirectoryEntries("*.json").stream().map {
            return@map Group(Json.decodeFromString(Files.readString(it)))
        }.toList()
    }

    /**
     * Return all cached Items
     */
    override fun items(): List<Group> {
        return this.cachedGroups
    }

    /**
     * Return the cached Item with the given identifier
     */
    override fun item(identifier: String): Group? {
        return this.cachedGroups.stream().filter { it.data.name == identifier }.findFirst().orElse(null)
    }

    override fun publish(group: Group) {
        Files.writeString(STORAGE_PATH.resolve(group.data.name + ".json"), Json.encodeToString(group.data))
    }

    override fun destroy(group: Group) {
        TODO("Not yet implemented")
    }
}