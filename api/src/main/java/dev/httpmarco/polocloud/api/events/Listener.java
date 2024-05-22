package dev.httpmarco.polocloud.api.events;

final class Listener<T extends Event> {
    private final Class<? extends T> event;
    private final EventRunnable<T> runnable;

    public Listener(Class<? extends T> event, EventRunnable<T> runnable) {
        this.event = event;
        this.runnable = runnable;
    }

    public Class<? extends T> getEvent() {
        return event;
    }
    public EventRunnable<T> getRunnable() {
        return runnable;
    }
}
