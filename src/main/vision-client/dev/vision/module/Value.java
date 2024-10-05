package dev.vision.module;

import dev.vision.module.values.BooleanValue;
import me.empty.api.event.handler.EventManager;
import me.empty.japi.point.Point3f;

import java.util.function.Supplier;

public class Value<V> {
    private final String name;

    private V value;

    private final Supplier<Boolean> supplier;

    public Value(String name, V value, Supplier<Boolean> supplier) {
        this.name = name;
        this.supplier = supplier;
        this.value = value;
        EventManager.register(this);
    }

    public String getName() {
        return name;
    }

    private final Point3f clickGuiAnim = new Point3f(0, 0, 0);

    public Point3f getClickGuiAnim() {
        return clickGuiAnim;
    }

    public boolean isVisitable() {
        return !this.supplier.get();
    }

    public V getValue() {
        if (this instanceof BooleanValue && isVisitable()) {
            return (V) Boolean.FALSE;
        }
        return value;
    }

    public void setValue(V value) {
        this.value = value;
        this.onChanged();
    }

    public void onChanged() {
    }
}

