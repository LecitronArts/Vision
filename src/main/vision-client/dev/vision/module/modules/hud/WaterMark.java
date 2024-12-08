package dev.vision.module.modules.hud;

import dev.vision.Vision;
import dev.vision.events.EventRender2D;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import me.empty.api.event.component.EventTarget;

import java.awt.*;

@ModuleInfo(name = "WaterMark", displayName = "Water Mark", category = Category.HUD, enableOnStartUp = true)
public class WaterMark extends BasicModule {
    @EventTarget
    private void onRender2D(EventRender2D event) {
        float x = 8;
        float y = 8;

        String name = Vision.INSTANCE.CLIENT_NAME;
        String version = Vision.INSTANCE.CLIENT_VERSION;


    }
}
