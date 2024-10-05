package dev.vision.module.modules.movement;

import dev.vision.Vision;
import dev.vision.events.EventTick;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import dev.vision.util.MovementUtils;
import me.empty.api.event.component.EventTarget;
import net.minecraft.client.KeyMapping;

@ModuleInfo(name = "Sprint", displayName = "Sprint", category = Category.Movement, enableOnStartUp = true)
public class Sprint extends BasicModule {
    public static boolean allow_sprint = true;

    @EventTarget
    private void onTick(EventTick event) {
        if (mc.player == null) {
            return;
        }

        if (Sprint.allow_sprint && MovementUtils.isMoving() && !mc.player.isShiftKeyDown() && !mc.player.horizontalCollision) {
            KeyMapping.set(mc.options.keySprint.getDefaultKey(), true);
            mc.options.toggleSprint().set(false);
            if (mc.player.zza > 0) {
                if (Vision.INSTANCE.moduleManager.getModule(NoSlow.class).isEnabled() && mc.player.isUsingItem()) {
                    mc.player.setSprinting(true);
                }
            }
        } else {
            KeyMapping.set(mc.options.keySprint.getDefaultKey(), false);
            mc.options.toggleSprint().set(false);
            mc.player.setSprinting(false);
            Sprint.allow_sprint = true;
        }
    }
}
