package dev.vision.module.values;

import dev.vision.module.Value;
import java.util.function.Supplier;

public class NumberValue extends Value<Number> {
    private final Number minimum;
    private final Number maximum;
    private final Number increment;

    public NumberValue(String name, Number value, Number min, Number max, Number inc) {
        super(name, value.doubleValue(), () -> true);
        this.minimum = min.doubleValue();
        this.maximum = max.doubleValue();
        this.increment = inc.doubleValue();
    }

    public NumberValue(String name, Number value, Number min, Number max, Number inc, Supplier<Boolean> visitable) {
        super(name, value.doubleValue(), visitable);
        this.minimum = min.doubleValue();
        this.maximum = max.doubleValue();
        this.increment = inc.doubleValue();
    }

    public Number getMinimum() {
        return minimum;
    }

    public Number getMaximum() {
        return maximum;
    }

    public Number getIncrement() {
        return increment;
    }
}

