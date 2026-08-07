package de.polocloud.api.group

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Public entry point to the cluster's group API.
 *
 * Backed by a [GroupApiClient] (gRPC in production). Obtain the shared instance
 * via [de.polocloud.api.Polocloud.groupService].
 *
 * Most methods have a non-blocking `*Async` counterpart returning a [CompletableFuture] —
 * see [de.polocloud.api.services.ServiceService]'s class doc for when to prefer it. `create`
 * and `edit` don't (yet): both go through [GroupBuilder.submit], whose blocking call would
 * need its own async counterpart first.
 */
class GroupService internal constructor(
    private val client: GroupApiClient,
) {

    // Backs every `*Async` method's future.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun findAll(): List<Group> =
        runBlocking(Dispatchers.IO) { client.findGroups(null, null) }.map(GroupMapper::toApi)

    /** Non-blocking form of [findAll]. */
    fun findAllAsync(): CompletableFuture<List<Group>> =
        scope.future { client.findGroups(null, null).map(GroupMapper::toApi) }

    fun find(name: String): Group? =
        runBlocking(Dispatchers.IO) { client.findGroups(name, null) }
            .map(GroupMapper::toApi)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** Non-blocking form of [find]. */
    fun findAsync(name: String): CompletableFuture<Group?> =
        scope.future {
            client.findGroups(name, null).map(GroupMapper::toApi).firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

    fun find(type: GroupFilterType): List<Group> =
        findAll().filter { type.matches(it.platform) }

    /** Non-blocking form of [find]. */
    fun findAsync(type: GroupFilterType): CompletableFuture<List<Group>> =
        scope.future { client.findGroups(null, null).map(GroupMapper::toApi).filter { type.matches(it.platform) } }

    fun count(): Int = findAll().size

    /** Non-blocking form of [count]. */
    fun countAsync(): CompletableFuture<Int> = scope.future { client.findGroups(null, null).size }

    fun delete(name: String) {
        runBlocking(Dispatchers.IO) { client.deleteGroup(name) }
    }

    /** Non-blocking form of [delete]. */
    fun deleteAsync(name: String): CompletableFuture<Void?> = scope.future { client.deleteGroup(name); null }

    fun delete(group: Group) = delete(group.name)

    /** Non-blocking form of [delete]. */
    fun deleteAsync(group: Group): CompletableFuture<Void?> = deleteAsync(group.name)

    /** Ends every in-flight `*Async` call and releases background resources. */
    fun close() = scope.cancel()

    /**
     * Edits the group named [name].
     *
     * The [editor] receives a builder pre-filled with the group's current values, so
     * any field left untouched is resubmitted unchanged instead of being reset to a
     * builder default.
     *
     * @throws NoSuchElementException if no group named [name] exists.
     */
    fun edit(name: String, editor: Consumer<GroupBuilder>) {
        val current = find(name) ?: throw NoSuchElementException("No group named '$name' exists")
        val builder = GroupBuilder(current) { group ->
            GroupMapper.toApi(runBlocking { client.updateGroup(GroupMapper.toProto(group)) })
        }
        editor.accept(builder)
        builder.submit()
    }

    fun create(name: String): GroupBuilder =
        GroupBuilder { group ->
            GroupMapper.toApi(runBlocking { client.createGroup(GroupMapper.toProto(group)) })
        }.name(name)
}
