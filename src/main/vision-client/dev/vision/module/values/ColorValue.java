package dev.vision.module.values;

import dev.vision.module.Value;

import java.awt.*;
import java.util.function.Supplier;

public class ColorValue extends Value<Color> {
    public boolean edit;

    public ColorValue(String name, Color value) {
        super(name, value, () -> true);
    }

    public ColorValue(String name, Color value, Supplier<Boolean> visitable) {
        super(name, value, visitable);
    }
}
