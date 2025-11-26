package dev.httpmarco.polocloud.agent.runtime.abstracts

import com.google.gson.GsonBuilder
import dev.httpmarco.polocloud.agent.groups.AbstractGroup
import dev.httpmarco.polocloud.agent.i18n
import dev.httpmarco.polocloud.agent.runtime.RuntimeGroupStorage
import dev.httpmarco.polocloud.shared.properties.PropertyHolder
import dev.httpmarco.polocloud.shared.properties.PropertySerializer
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.shared.template.TemplateSerializer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

abstract class AbstractGroupStorage(val path: Path = Path("local/groups")) : RuntimeGroupStorage {

    private lateinit var cachedAbstractGroups: ArrayList<AbstractGroup>

    private val STORAGE_GSON = GsonBuilder().setPrettyPrinting()
        .registerTypeHierarchyAdapter(PropertyHolder::class.java, PropertySerializer())
        .registerTypeAdapter(PropertyHolder::class.java, PropertySerializer())
        .registerTypeHierarchyAdapter(Template::class.java, TemplateSerializer())
        .registerTypeAdapter(Template::class.java, TemplateSerializer())
        .create()

    init {
        this.initialize()
    }

    private fun initialize() {
        path.createDirectories()

        // load all groups from the storage path
        cachedAbstractGroups = ArrayList(path.listDirectoryEntries("*.json").stream().map {
            return@map STORAGE_GSON.fromJson(Files.readString(it), AbstractGroup::class.java)
        }.toList())
    }

    override fun findAll(): List<AbstractGroup> {
        return this.cachedAbstractGroups
    }

    override fun findAllAsync() = CompletableFuture.completedFuture(findAll())

    override fun find(name: String): AbstractGroup? {
        return this.cachedAbstractGroups.stream().filter { it.name == name }.findFirst().orElse(null)
    }

    override fun findAsync(name: String) = CompletableFuture.completedFuture<AbstractGroup?>(find(name))

    override fun create(group: AbstractGroup): AbstractGroup? {
        if (this.find(group.name) != null) {
            return null // group already exists
        }
        Files.writeString(groupPath(group), STORAGE_GSON.toJson(group))
        this.cachedAbstractGroups.add(group)
        return group
    }

    override fun createAsync(group: AbstractGroup): CompletableFuture<AbstractGroup?> {
        return CompletableFuture.completedFuture(create(group))
    }

    override fun update(group: AbstractGroup): AbstractGroup? {
        if(this.find(group.name) == null) {
            return null
        }
        // overwrite the existing group file with the new data
        Files.writeString(groupPath(group), STORAGE_GSON.toJson(group))
        // update the cached group
        val index = this.cachedAbstractGroups.indexOfFirst { it.name == group.name }
        if (index != -1) {
            this.cachedAbstractGroups[index] = group
        }
        return group
    }

    override fun updateAsync(group: AbstractGroup): CompletableFuture<AbstractGroup?> {
        return CompletableFuture.completedFuture(update(group))
    }

    override fun delete(name: String): AbstractGroup? {
        val group = this.find(name) ?: return null
        this.cachedAbstractGroups.remove(group)
        this.groupPath(group).deleteIfExists()
        return group
    }

    private fun groupPath(abstractGroup: AbstractGroup): Path {
        return path.resolve(abstractGroup.name + ".json")
    }

    override fun reload() {
        i18n.info("agent.local-runtime.group-storage.reload")
        this.initialize()
        i18n.info("agent.local-runtime.group-storage.collect", this.cachedAbstractGroups.size)
    }
}