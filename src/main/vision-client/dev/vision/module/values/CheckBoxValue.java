package dev.vision.module.values;

import dev.vision.module.Value;

import java.util.Arrays;
import java.util.function.Supplier;

public class CheckBoxValue extends Value<BooleanValue> {
    private final BooleanValue[] values;
    private boolean show;

    public CheckBoxValue(String name, BooleanValue[] values) {
        super(name, null, () -> true);
        this.values = values;
        this.show = false;
    }

    public CheckBoxValue(String name, BooleanValue[] values, Supplier<Boolean> visitable) {
        super(name, null, visitable);
        this.values = values;
        this.show = false;
    }

    public int getEnables() {
        return Arrays.asList(values).parallelStream().mapToInt((value) -> value.getValue() ? 1 : 0).sum();
    }

    public boolean getValue(String name) {
        return Arrays.stream(values).anyMatch(booleanValue -> booleanValue.getName().equals(name) && booleanValue.getValue());
    }

    public BooleanValue[] getValues() {
        return values;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }
}