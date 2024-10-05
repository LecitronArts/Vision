package dev.vision.module.values;

import dev.vision.module.Value;

import java.util.function.Supplier;

public class ModeValue extends Value<String>{
    private final String[] modes;
    public boolean show = false;

    public ModeValue(String name, String[] modes, String value) {
        super(name, value, () -> true);
        this.modes = modes;
    }

    public ModeValue(String name, String[] modes, String value, Supplier<Boolean> visitable) {
        super(name, value, visitable);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    public boolean is(String value) {
        return this.getValue().equals(value);
    }
}
