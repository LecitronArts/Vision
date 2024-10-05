package dev.vision.module.modules.movement;

import dev.vision.events.EventTick;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import me.empty.api.event.component.EventTarget;
import net.minecraft.client.KeyMapping;

@ModuleInfo(name = "Sprint", displayName = "Sprint", category = Category.Movement, enableOnStartUp = true)
public class Sprint extends BasicModule {
    @EventTarget
    private void onTick(EventTick event) {
        if (mc.player == null) {
            return;
        }
        if (!mc.player.horizontalCollision && mc.player.zza > 0) {
            mc.options.toggleSprint().set(true);
            KeyMapping.set(mc.options.keySprint.getDefaultKey(), true);
        }
    }
}
