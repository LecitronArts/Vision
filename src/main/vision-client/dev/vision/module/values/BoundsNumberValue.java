package dev.vision.module.values;

import dev.vision.module.Value;
import me.empty.japi.bounds.Bounds;

import java.util.function.Supplier;

public class BoundsNumberValue extends Value<Bounds> {
    private final Number minimum;
    private final Number maximum;
    private final Number increment;

    public boolean bindMin = false;
    public boolean bindMax = false;

    public BoundsNumberValue(String name, Bounds bounds, Number min, Number max, Number inc) {
        super(name, bounds, () -> true);
        this.minimum = min.doubleValue();
        this.maximum = max.doubleValue();
        this.increment = inc.doubleValue();
    }

    public BoundsNumberValue(String name, Bounds bounds, Number min, Number max, Number inc, Supplier<Boolean> visitable) {
        super(name, bounds, visitable);
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

