package dev.httpmarco.polocloud.modules.rest.socket.web.impl.v3.service

import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.agent.services.AbstractService
import dev.httpmarco.polocloud.modules.rest.socket.BaseWebSocket
import dev.httpmarco.polocloud.modules.rest.socket.SocketSender
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceLogEvent
import io.javalin.http.Context
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsMessageContext
import java.io.InterruptedIOException

class ServiceScreenWebSocket : BaseWebSocket("/service/{serviceName}/screen", "polocloud.service.screen"), SocketSender {

    override fun onConnect(context: WsConnectContext) {
        this.clients += context

        val serviceName = context.pathParam("serviceName")

        val service = Agent.serviceProvider().find(serviceName)

        if (service == null) {
            context.closeSession(1008, "Service not found")
            return
        }

        try {
            (service as AbstractService).logs(5000).forEach {
                try {
                    if (context.session.isOpen) {
                        context.send(it)
                    }
                } catch (e: InterruptedIOException) {
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: InterruptedIOException) {
            // Service is stopping, ignore the error
            return
        } catch (e: Exception) {
            // Log other errors if needed
        }

        Agent.eventProvider().subscribe(ServiceLogEvent::class.java, { event ->
            try {
                if (event.service == service && context.session.isOpen) {
                    context.send(event.line)
                }
            } catch (e: InterruptedIOException) {
                // Service is stopping, ignore the error
            } catch (e: Exception) {
                // Log other errors if needed
            }
        })

        context.enableAutomaticPings()
    }

    override fun onClose(context: WsCloseContext) {
        this.clients -= context
        //TODO remove subscriptions
    }

    override fun onMessage(context: WsMessageContext) {
    }

    override fun onError(context: Context) {
    }

    override fun send(message: String) {
        this.clients.forEach { client ->
            try {
                if (client.session.isOpen) {
                    client.send(message)
                }
            } catch (e: InterruptedIOException) {
                // Service is stopping, ignore the error
            } catch (e: Exception) {
                // Log other errors if needed, but don't crash
            }
        }
    }
}