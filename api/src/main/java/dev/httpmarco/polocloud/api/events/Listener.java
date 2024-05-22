package dev.httpmarco.polocloud.api.events;

public record Listener<T extends Event>(Class<? extends T> event, EventRunnable<T> runnable) {

}
