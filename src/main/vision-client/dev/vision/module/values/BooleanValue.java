package dev.vision.module.values;

import dev.vision.module.Value;

import java.util.function.Supplier;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, Boolean enabled) {
        super(name, enabled, () -> true);
    }

    public BooleanValue(String name, Boolean enabled, Supplier<Boolean> visitable) {
        super(name, enabled, visitable);
    }
}