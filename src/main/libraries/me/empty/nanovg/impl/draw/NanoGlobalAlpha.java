package me.empty.nanovg.impl.draw;

import me.empty.nanovg.interfaces.INano;
import org.lwjgl.nanovg.NanoVG;

public class NanoGlobalAlpha implements INano {
    public static void alpha(float alpha, Runnable runnable) {
        start(alpha);
        runnable.run();
        end();
    }

    public static void start(float alpha) {
        NanoVG.nvgGlobalAlpha(nvg, alpha);
    }

    public static void end() {
        NanoVG.nvgGlobalAlpha(nvg, 1.0f);
    }
}
