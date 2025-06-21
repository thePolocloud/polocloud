package dev.httpmarco.polocloud.agent.runtime.local

import dev.httpmarco.polocloud.agent.groups.Group
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

private val STORAGE_PATH = Path("local/groups")

class LocalRuntimeGroupStorage : RuntimeGroupStorage {

    private var cachedGroups: MutableList<Group>

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
        Files.writeString(groupPath(group), Json.encodeToString(group.data))
        this.cachedGroups.add(group)
    }

    override fun destroy(group: Group) {
        this.cachedGroups.remove(group)
        this.groupPath(group).deleteIfExists()
    }

    private fun groupPath(group: Group): Path {
        return STORAGE_PATH.resolve(group.data.name + ".json")
    }
}