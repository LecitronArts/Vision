package dev.vision.events;

import me.empty.api.event.Cancelable;

public class EventKeyPress extends Cancelable {
    private final int key;

    public EventKeyPress(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
