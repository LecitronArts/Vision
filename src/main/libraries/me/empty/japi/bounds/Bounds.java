package me.empty.japi.bounds;

import me.empty.japi._JApi;

public class Bounds {
    private Number min;
    private Number max;

    public Bounds(Number min, Number max) {
        this.min = min;
        this.max = max;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public Number getRandom() {
        return min.doubleValue() + (_JApi.random.nextFloat() * (max.doubleValue() - min.doubleValue()));
    }
}
