package me.empty.nanovg.impl.draw;

import org.lwjgl.nanovg.NanoVG;

import static me.empty.nanovg.interfaces.INano.nvg;

public class NanoRotateUtil {
    /**
     * Executes a rotation transformation around the specified center, followed by the execution of the given runnable.
     * <p>
     * Saves the current graphics state, applies a rotation transformation around the given center and angle,
     * runs the provided runnable, and then restores the previous graphics state.
     *
     * @param centerX The x-coordinate of the rotation center.
     * @param centerY The y-coordinate of the rotation center.
     * @param angle The angle of rotation in degrees.
     * @param runnable The runnable to execute within the rotation transformation.
     */
    public static void rotate(float centerX, float centerY, float angle, Runnable runnable) {
        start(centerX, centerY, angle);
        runnable.run();
        end();
    }

    /**
     * Executes a rotation transformation around the specified center, with a dynamically calculated angle, followed by the execution of the given runnable.
     * <p>
     * Calculates an angle based on the current system time, applies a rotation transformation around the given center,
     * runs the provided runnable, and then restores the previous graphics state.
     *
     * @param centerX The x-coordinate of the rotation center.
     * @param centerY The y-coordinate of the rotation center.
     * @param runnable The runnable to execute within the rotation transformation.
     */
    public static void rotate(float centerX, float centerY, Runnable runnable) {
        start(centerX, centerY);
        runnable.run();
        end();
    }

    /**
     * Starts a new rotation transformation.
     * <p>
     * Saves the current graphics state, translates the origin to the specified centerX and centerY,
     * and then rotates the canvas by the given angle in degrees.
     *
     * @param centerX The x-coordinate of the rotation center.
     * @param centerY The y-coordinate of the rotation center.
     * @param angle The angle of rotation in degrees.
     */
    public static void start(float centerX, float centerY, float angle) {
        NanoVG.nvgSave(nvg);
        NanoVG.nvgTranslate(nvg, centerX, centerY);
        NanoVG.nvgRotate(nvg, angle);
        NanoVG.nvgTranslate(nvg, -centerX, -centerY);
    }

    /**
     * Starts a new rotation transformation with a dynamically calculated angle.
     * <p>
     * Calculates an angle based on the current system time (modulo 36000 milliseconds to cycle through 360 degrees, divided by 100 for smoother animation),
     * and then calls the 'start' method with this angle and the specified center coordinates.
     *
     * @param centerX The x-coordinate of the rotation center.
     * @param centerY The y-coordinate of the rotation center.
     */
    public static void start(float centerX, float centerY) {
        float angle = System.currentTimeMillis() % 36000 / 100.0f;
        start(centerX, centerY, angle);
    }

    /**
     * Ends the current rotation transformation.
     * <p>
     * Restores the previous graphics state, effectively cancelling any transformations made after the last 'nvgSave' call.
     */
    public static void end() {
        NanoVG.nvgRestore(nvg);
    }
}
