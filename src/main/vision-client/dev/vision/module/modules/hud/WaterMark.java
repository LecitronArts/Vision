package dev.vision.module.modules.hud;

import dev.vision.Vision;
import dev.vision.events.EventRender2D;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import dev.vision.util.font.FontLoaders;
import me.empty.api.event.component.EventTarget;
import me.empty.nanovg.NanoVGHelper;
import me.empty.nanovg.impl.draw.NanoRender2DUtil;

import java.awt.*;

@ModuleInfo(name = "WaterMark", displayName = "Water Mark", category = Category.HUD, enableOnStartUp = true)
public class WaterMark extends BasicModule {
    @EventTarget
    private void onRender2D(EventRender2D event) {
        float x = 8;
        float y = 8;

        String name = Vision.INSTANCE.CLIENT_NAME;
        String version = Vision.INSTANCE.CLIENT_VERSION;

        NanoVGHelper.INSTANCE.begin();
        NanoRender2DUtil.drawRound(x, y, 2, 18, 2, 2, 0, 0, new Color(-1));
        NanoRender2DUtil.drawRound(x + 2, y, 6 + FontLoaders.neverlose_18.getStringWidth(name) + 12 + FontLoaders.neverlose_18.getStringWidth(version) + 6, 18, 0, 0, 5, 5, new Color(0, 0, 0, 120));
        FontLoaders.neverlose_18.drawStringWithGlow(name, x + 6, y + 5, new Color(-1));
        NanoRender2DUtil.drawRect(x + 6 + FontLoaders.neverlose_18.getStringWidth(name) + 6, y + 4, 1, 10, new Color(-1));
        FontLoaders.neverlose_18.drawStringWithShadow(version, x + 6 + FontLoaders.neverlose_18.getStringWidth(name) + 12, y + 5, new Color(-1));
        NanoVGHelper.INSTANCE.end();
    }
}
