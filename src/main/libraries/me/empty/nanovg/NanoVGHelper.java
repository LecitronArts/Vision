package me.empty.nanovg;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;

public class NanoVGHelper {
    public static final NanoVGHelper INSTANCE = new NanoVGHelper();

    private long nvgContext = -1;

    public void create() {
        if (nvgContext == -1) {
            nvgContext = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS);
            if (nvgContext == -1) {
                throw new RuntimeException("Failed to create nvg context");
            }
        }
    }

    public void begin(float windowWidth, float windowHeight, float scaleFactor) {
        this.create();
        NanoVG.nvgBeginFrame(nvgContext, windowWidth / scaleFactor, windowHeight / scaleFactor, scaleFactor);
    }

    public void begin() {
        Minecraft mc = Minecraft.getInstance();
        GlStateManager.disableAlphaTest();
        this.begin(mc.getWindow().getWidth(), mc.getWindow().getHeight(), (float) mc.getWindow().getGuiScale());
    }

    public void end() {
        NanoVG.nvgEndFrame(nvgContext);
        GlStateManager.enableAlphaTest();
    }

    public long getContext() {
        return nvgContext;
    }
}