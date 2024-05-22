package dev.httpmarco.polocloud.api.events;

import java.util.ArrayList;
import java.util.List;

public class EventNode<T extends Event> {

    private final List<Listener<T>> listeners = new ArrayList<>();
    private final List<EventNode<T>> children = new ArrayList<>();

    public void call(T event){
        listeners.stream()
                .filter(listener -> listener.event().equals(event.getClass()))
                .forEach(listener -> listener.runnable().run(event));

        children.stream().forEach(children -> children.call(event));
    }

    public void addListener(Class<? extends T> event, EventRunnable<T> runnable){
        listeners.add(new Listener<T>(event, runnable));
    }

    public EventNode<T> addChildren(EventNode<T> eventNode){
        children.add(eventNode);
        return this;
    }

    public EventNode<T> removeChildren(EventNode<T> eventNode){
        this.children.remove(eventNode);
        return this;
    }
}
