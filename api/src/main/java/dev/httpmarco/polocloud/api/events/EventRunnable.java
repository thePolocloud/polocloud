package dev.httpmarco.polocloud.api.events;

public interface EventRunnable<T extends Event> {

    void run(T event);

}
