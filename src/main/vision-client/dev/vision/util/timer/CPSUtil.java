package dev.vision.util.timer;

public class CPSUtil {
    private long lastMS;

    public CPSUtil() {
        this.reset();
    }

    public void reset() {
        this.lastMS = System.nanoTime() / 1000000L;
    }

    public boolean should(float cps) {
        return (double) System.nanoTime() / 1000000L - this.lastMS >= (1000.0 / (cps * 1.5));
    }
}
