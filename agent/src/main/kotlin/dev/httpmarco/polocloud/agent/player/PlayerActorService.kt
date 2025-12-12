package dev.httpmarco.polocloud.agent.player


import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.v1.player.StreamingAlert
import io.grpc.stub.ServerCallStreamObserver

class PlayerActorService {

    // Additional properties
    var actorStream: ServerCallStreamObserver<StreamingAlert>? = null

    fun isActive(): Boolean {
        return actorStream != null && !actorStream!!.isCancelled
    }

    fun updateStream(observer: ServerCallStreamObserver<StreamingAlert>) {
        this.actorStream = observer
    }

    fun stream(context: StreamingAlert) {
        if (this.actorStream == null || this.actorStream!!.isCancelled) {
            logger.warn("Attempted to stream to a null or cancelled actor stream.")
            return
        }
        this.actorStream!!.onNext(context)
    }
}