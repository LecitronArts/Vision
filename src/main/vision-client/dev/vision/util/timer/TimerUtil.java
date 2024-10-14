package dev.vision.util.timer;

public class TimerUtil {
    private long lastTime;

    public TimerUtil() {
        this.reset();
    }

    public void reset() {
        this.lastTime = System.currentTimeMillis();
    }

    public long getTimePassed() {
        return System.currentTimeMillis() - this.lastTime;
    }

    public boolean hasTimePassed(long time) {
        return getTimePassed() >= time;
    }

    public void setTime(long time) {
        this.lastTime = time;
    }
}
