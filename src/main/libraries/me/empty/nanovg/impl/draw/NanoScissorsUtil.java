package me.empty.nanovg.impl.draw;

import me.empty.nanovg.interfaces.INano;
import org.lwjgl.nanovg.NanoVG;

public class NanoScissorsUtil implements INano {
    public static void start(float x, float y, float width, float height) {
        NanoVG.nvgSave(nvg);
        NanoVG.nvgScissor(nvg, x, y, width, height);
    }

    public static void end() {
        NanoVG.nvgResetScissor(nvg);
        NanoVG.nvgRestore(nvg);
    }
}
