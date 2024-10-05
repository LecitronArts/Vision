package me.empty.nanovg.impl.draw;

import me.empty.nanovg.interfaces.INano;
import org.lwjgl.nanovg.NanoVG;

public class NanoScaleUtil implements INano {
    /**
     * Applies a uniform scale factor to the canvas and executes the provided runnable.
     * After the runnable is executed, the scaling is reset.
     *
     * @param scale The uniform scale factor to apply
     * @param runnable The runnable to execute within the scaled context
     */
    public static void scale(float scale, Runnable runnable) {
        start(scale);
        runnable.run();
        end();
    }

    /**
     * Applies a uniform scale factor to the canvas around a specified center point and executes the provided runnable.
     * After the runnable is executed, the scaling is reset.
     *
     * @param x The x-coordinate of the scaling center point
     * @param y The y-coordinate of the scaling center point
     * @param scale The uniform scale factor to apply
     * @param runnable The runnable to execute within the scaled context
     */
    public static void scale(float x, float y, float scale, Runnable runnable) {
        start(x, y, scale);
        runnable.run();
        end();
    }

    /**
     * Applies a uniform scale factor to the canvas around the center of a specified rectangle and executes the provided runnable.
     * After the runnable is executed, the scaling is reset.
     *
     * @param x The x-coordinate of the top-left corner of the rectangle
     * @param y The y-coordinate of the top-left corner of the rectangle
     * @param width The width of the rectangle
     * @param height The height of the rectangle
     * @param scale The uniform scale factor to apply
     * @param runnable The runnable to execute within the scaled context
     */
    public static void scale(float x, float y, float width, float height, float scale, Runnable runnable) {
        start(x, y, width, height, scale);
        runnable.run();
        end();
    }

    /**
     * @param scale The scale factor
     */
    public static void start(float scale) {
        NanoVG.nvgScale(nvg, scale, scale);
    }

    /**
     * Start scaling operation at the specified point (x, y).
     * @param x The x-coordinate of the scaling center point
     * @param y The y-coordinate of the scaling center point
     * @param scale The scale factor
     */
    public static void start(float x, float y, float scale) {
        NanoVG.nvgTranslate(nvg, x, y);
        NanoVG.nvgScale(nvg, scale, scale);
        NanoVG.nvgTranslate(nvg, -x, -y);
    }

    /**
     * Start scaling operation at the center point of the specified rectangle (x, y, width, height).
     * @param x The x-coordinate of the top-left corner of the rectangle
     * @param y The y-coordinate of the top-left corner of the rectangle
     * @param width The width of the rectangle
     * @param height The height of the rectangle
     * @param scale The scale factor
     */
    public static void start(float x, float y, float width, float height, float scale) {
        NanoVG.nvgTranslate(nvg, x + (width / 2F), y + (height / 2F));
        NanoVG.nvgScale(nvg, scale, scale);
        NanoVG.nvgTranslate(nvg, -(x + (width / 2F)), -(y + (height / 2F)));
    }

    /**
     * End the scaling operation and reset the transformation.
     */
    public static void end() {
        NanoVG.nvgResetTransform(nvg);
    }
}
