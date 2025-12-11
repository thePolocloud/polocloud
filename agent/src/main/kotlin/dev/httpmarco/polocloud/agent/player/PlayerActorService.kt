package dev.httpmarco.polocloud.agent.player

import com.google.protobuf.Any
import dev.httpmarco.polocloud.agent.logger
import dev.httpmarco.polocloud.v1.proto.EventProviderOuterClass
import io.grpc.stub.ServerCallStreamObserver

class PlayerActorService {

    // Additional properties
    var actorStream: ServerCallStreamObserver<Any>? = null

    fun isActive(): Boolean {
        return actorStream != null && !actorStream!!.isCancelled
    }

    fun updateStream(observer: ServerCallStreamObserver<Any>) {
        this.actorStream = observer
    }

    fun stream(context: Any?) {
        if (this.actorStream == null || this.actorStream!!.isCancelled) {
            logger.warn("Attempted to stream to a null or cancelled actor stream.")
            return
        }
        this.actorStream!!.onNext(context)
    }
}