package dev.httpmarco.polocloud.modules.rest.socket.web.impl.v3.service

import com.google.gson.JsonObject
import dev.httpmarco.polocloud.agent.Agent
import dev.httpmarco.polocloud.modules.rest.socket.BaseWebSocket
import dev.httpmarco.polocloud.modules.rest.socket.SocketSender
import dev.httpmarco.polocloud.shared.events.definitions.service.ServiceChangeStateEvent
import dev.httpmarco.polocloud.shared.service.Service
import io.javalin.http.Context
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsMessageContext
import java.io.InterruptedIOException

class EventWebSocket : BaseWebSocket("/services/update", "polocloud.ws.service.update"), SocketSender {

    override fun onConnect(context: WsConnectContext) {
        this.clients += context

        Agent.eventProvider().subscribe(ServiceChangeStateEvent::class.java) { event ->
            try {
                if (context.session.isOpen) {
                    update(event.service)
                }
            } catch (e: InterruptedIOException) {
                // Service is stopping, ignore the error
            } catch (e: Exception) {
                // Log other errors if needed, but don't crash
            }
        }

        context.enableAutomaticPings()
    }

    override fun onClose(context: WsCloseContext) {
        this.clients -= context
    }

    override fun onMessage(context: WsMessageContext) {
    }

    override fun onError(context: Context) {
    }

    fun update(service: Service) {
        try {
            val message = JsonObject().apply {
                addProperty("serviceName", service.name())
                addProperty("state", service.state.name)
                addProperty("timestamp", System.currentTimeMillis().toString())
            }

            send(message.toString())
        } catch (e: InterruptedIOException) {
            // Service is stopping, ignore the error silently
        } catch (e: Exception) {
            // Log other errors if needed, but don't crash
        }
    }

    override fun send(message: String) {
        this.clients.forEach { client ->
            try {
                if (client.session.isOpen) {
                    client.send(message)
                }
            } catch (e: InterruptedIOException) {
                // Service is stopping, ignore the error silently
            } catch (e: Exception) {
                // Log other errors if needed, but don't crash
            }
        }
    }
}