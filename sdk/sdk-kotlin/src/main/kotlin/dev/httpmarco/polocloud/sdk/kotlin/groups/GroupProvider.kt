package dev.httpmarco.polocloud.sdk.kotlin.groups

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dev.httpmarco.polocloud.v1.proto.GroupControllerGrpc
import dev.httpmarco.polocloud.v1.proto.GroupProvider
import io.grpc.ManagedChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class GroupProvider(channel: ManagedChannel?) {

    private val directExecutor: Executor = MoreExecutors.directExecutor()
    private val groupStub = GroupControllerGrpc.newBlockingStub(channel)
    private val asyncGroupStub = GroupControllerGrpc.newFutureStub(channel)

    fun find() : List<Group> {
        return groupStub.find(GroupProvider.FindGroupRequest.newBuilder().build()).groupsList.stream().map { group -> Group(group) }.toList()
    }

    fun find(name: String) : Group? {
        return groupStub.find(GroupProvider.FindGroupRequest.newBuilder().setName(name).build()).groupsList.stream().map { group -> Group(group) }.findFirst().orElse(null)
    }

    fun findAsync(): CompletableFuture<List<Group>> {
        val request = GroupProvider.FindGroupRequest.newBuilder().build()
        val futureResponse = asyncGroupStub.find(request)

        return completableFromGuava(futureResponse) {
            it.groupsList.map { group -> Group(group) }
        }
    }

    fun findAsync(name: String): CompletableFuture<Group?> {
        val request = GroupProvider.FindGroupRequest.newBuilder().setName(name).build()
        val futureResponse = asyncGroupStub.find(request)

        return completableFromGuava(futureResponse) {
            it.groupsList.map { group -> Group(group) }.firstOrNull()
        }
    }


    private fun <T, R> completableFromGuava(guavaFuture: ListenableFuture<T>, mapper: (T) -> R): CompletableFuture<R> {
        val completable = CompletableFuture<R>()
        guavaFuture.addListener({
            try {
                val result = mapper(guavaFuture.get())
                completable.complete(result)
            } catch (ex: Exception) {
                completable.completeExceptionally(ex)
            }
        }, directExecutor)
        return completable
    }
}