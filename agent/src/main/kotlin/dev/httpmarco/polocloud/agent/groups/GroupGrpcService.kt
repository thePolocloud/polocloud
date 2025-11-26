package dev.httpmarco.polocloud.agent.groups

import com.google.gson.JsonPrimitive
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.shared.platform.PlatformIndex
import dev.httpmarco.polocloud.shared.properties.PropertyHolder
import dev.httpmarco.polocloud.shared.template.Template
import dev.httpmarco.polocloud.v1.groups.DeleteGroupResponse
import dev.httpmarco.polocloud.v1.groups.FindGroupRequest
import dev.httpmarco.polocloud.v1.groups.FindGroupResponse
import dev.httpmarco.polocloud.v1.groups.GroupControllerGrpc
import dev.httpmarco.polocloud.v1.groups.GroupDeleteRequest
import dev.httpmarco.polocloud.v1.groups.GroupSnapshot
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

class GroupGrpcService : GroupControllerGrpc.GroupControllerImplBase() {

    override fun find(request: FindGroupRequest, responseObserver: StreamObserver<FindGroupResponse>) {

        val builder = FindGroupResponse.newBuilder()
        val groupStorage = Agent.runtime.groupStorage()

        val groupsToReturn = if (request.name.isNotEmpty()) {
            groupStorage.find(request.name)?.let { listOf(it) } ?: emptyList()
        } else {
            groupStorage.findAll()
        }

        for (group in groupsToReturn) {
            builder.addGroups(group.to())
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()

    }

    override fun create(request: GroupSnapshot, responseObserver: StreamObserver<GroupSnapshot>) {
        val groupStorage = Agent.runtime.groupStorage()

        if (groupStorage.find(request.name) != null) {
            responseObserver.onError(StatusRuntimeException(Status.ALREADY_EXISTS))
            return
        }

        val properties = PropertyHolder.empty()
        request.propertiesMap.forEach { (t, u) ->
            run {
                properties.raw(t, JsonPrimitive(u))
            }
        }

        val group = AbstractGroup(
            request.name,
            request.minMemory,
            request.maxMemory,
            request.minOnline,
            request.maxOnline,
            request.startThreshold,
            PlatformIndex(request.platform.name, request.platform.version),
            System.currentTimeMillis(),
            Template.fromSnapshotList(request.templatesList),
            properties
        )

        Agent.runtime.groupStorage().create(group)
        responseObserver.onNext(group.to())
        responseObserver.onCompleted()

    }

    override fun update(request: GroupSnapshot, responseObserver: StreamObserver<GroupSnapshot>) {
        val groupStorage = Agent.runtime.groupStorage()

        if (groupStorage.find(request.name) == null) {
            responseObserver.onError(StatusRuntimeException(Status.NOT_FOUND))
            return
        }

        val properties = PropertyHolder.empty()
        request.propertiesMap.forEach { (key, value) ->
            properties.raw(
                key, when {
                    value.lowercase().toBooleanStrictOrNull() != null -> JsonPrimitive(value.toBoolean())
                    value.toIntOrNull() != null -> JsonPrimitive(value.toInt())
                    value.toDoubleOrNull() != null -> JsonPrimitive(value.toDouble())
                    value.toFloatOrNull() != null -> JsonPrimitive(value.toFloat())
                    else -> JsonPrimitive(value)
                }
            )
        }

        val group = AbstractGroup(
            request.name,
            request.minMemory,
            request.maxMemory,
            request.minOnline,
            request.maxOnline,
            request.startThreshold,
            PlatformIndex(request.platform.name, request.platform.version),
            request.createdAt,
            Template.fromSnapshotList(request.templatesList),
            properties
        )

        Agent.runtime.groupStorage().update(group)
        responseObserver.onNext(group.to())
        responseObserver.onCompleted()
    }

    override fun delete(request: GroupDeleteRequest, responseObserver: StreamObserver<DeleteGroupResponse>) {

        val groupStorage = Agent.runtime.groupStorage()

        val group = groupStorage.find(request.name)
        if (group == null) {
            responseObserver.onError(StatusRuntimeException(Status.NOT_FOUND))
            return
        }

        Agent.runtime.serviceStorage().findByGroup(group).forEach { it.shutdown() }
        Agent.runtime.groupStorage().delete(group.name)
        responseObserver.onNext(DeleteGroupResponse.newBuilder().setDeleted(true).build())
        responseObserver.onCompleted()
    }
}