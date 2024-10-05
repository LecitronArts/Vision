package dev.vision.module.modules.movement;

import dev.vision.events.EventSlowChange;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import me.empty.api.event.component.EventTarget;

@ModuleInfo(name = "NoSlow", displayName = "No Slow", category = Category.Movement)
public class NoSlow extends BasicModule {
    @EventTarget
    private void onSlowSpeed(EventSlowChange event) {
        event.setSlow(1.0F);
    }
}
