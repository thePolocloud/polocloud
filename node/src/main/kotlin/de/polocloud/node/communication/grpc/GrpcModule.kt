package de.polocloud.node.communication.grpc

import de.polocloud.common.communication.server.executor.GrpcServerExecutor
import de.polocloud.common.communication.server.registery.GrpcServerHandlerRegistry
import de.polocloud.node.communication.grpc.middleware.ErrorServerMiddleware
import de.polocloud.node.communication.grpc.middleware.LoggingServerMiddleware
import de.polocloud.node.communication.grpc.middleware.NodeLastConnectionMiddleware
import de.polocloud.node.communication.handler.cluster.CreateTokenServerHandler
import de.polocloud.node.communication.handler.cluster.ListNodesServerHandler
import de.polocloud.node.communication.handler.group.CreateGroupServerHandler
import de.polocloud.node.communication.handler.group.DeleteGroupServerHandler
import de.polocloud.node.communication.handler.group.GetGroupInformationServerHandler
import de.polocloud.node.communication.handler.group.UpdateGroupServerHandler
import de.polocloud.node.communication.handler.node.GetNodeInformationServerHandler
import de.polocloud.node.communication.handler.services.ExecuteServiceCommandServerHandler
import de.polocloud.node.communication.handler.services.FindServicesServerHandler
import de.polocloud.node.communication.handler.services.GetServiceResourceUsageServerHandler
import de.polocloud.node.communication.handler.services.ListServicesServerHandler
import de.polocloud.node.communication.handler.services.StopGroupServicesServerHandler
import de.polocloud.node.communication.handler.services.StopServiceServerHandler
import de.polocloud.node.group.GroupService
import de.polocloud.node.services.ServiceProvider
import de.polocloud.proto.CreateGroupRequest
import de.polocloud.proto.CreateTokenRequest
import de.polocloud.proto.DeleteGroupRequest
import de.polocloud.proto.ExecuteServiceCommandRequest
import de.polocloud.proto.GetServiceResourceUsageRequest
import de.polocloud.proto.GroupListRequest
import de.polocloud.proto.ListNodesRequest
import de.polocloud.proto.ListServicesRequest
import de.polocloud.proto.NodeInformationRequest
import de.polocloud.proto.ServiceListRequest
import de.polocloud.proto.StopGroupServicesRequest
import de.polocloud.proto.StopServiceRequest
import de.polocloud.proto.UpdateGroupRequest

object GrpcModule {

    fun createExecutor(groupService: GroupService, serviceProvider: ServiceProvider): GrpcServerExecutor {
        val registry = GrpcServerHandlerRegistry().apply {
            register(ListNodesRequest::class.java, ListNodesServerHandler())
            register(NodeInformationRequest::class.java, GetNodeInformationServerHandler())
            register(ListServicesRequest::class.java, ListServicesServerHandler(serviceProvider))
            register(ServiceListRequest::class.java, FindServicesServerHandler(serviceProvider))
            register(StopServiceRequest::class.java, StopServiceServerHandler(serviceProvider))
            register(ExecuteServiceCommandRequest::class.java, ExecuteServiceCommandServerHandler(serviceProvider))
            register(StopGroupServicesRequest::class.java, StopGroupServicesServerHandler(serviceProvider))
            register(GetServiceResourceUsageRequest::class.java, GetServiceResourceUsageServerHandler(serviceProvider))
            register(CreateTokenRequest::class.java, CreateTokenServerHandler())
            register(GroupListRequest::class.java, GetGroupInformationServerHandler(groupService))
            register(CreateGroupRequest::class.java, CreateGroupServerHandler(groupService))
            register(UpdateGroupRequest::class.java, UpdateGroupServerHandler(groupService))
            register(DeleteGroupRequest::class.java, DeleteGroupServerHandler(groupService, serviceProvider))
        }

        return GrpcServerExecutor(
            registry,
            listOf(
                NodeLastConnectionMiddleware(),
                ErrorServerMiddleware(),
                LoggingServerMiddleware()
            )
        )
    }
}