package dev.vision.module.modules.movement;

import dev.vision.events.EventMotion;
import dev.vision.events.EventSlowChange;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import dev.vision.module.values.ModeValue;
import me.empty.api.event.component.EventTarget;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "NoSlow", displayName = "No Slow", category = Category.Movement)
public class NoSlow extends BasicModule {
    private final ModeValue mode = new ModeValue("Mode", new String[] {"Vanilla", "Grim"}, "Vanilla");

    private boolean shouldSlow;

    @EventTarget
    private void onSlowSpeed(EventSlowChange event) {
        assert mc.player != null;

        float speed = 0.2F;
        if (mode.is("Grim")) {
            boolean grim = mc.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE ||
                    mc.player.getMainHandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE ||
                    mc.player.getOffhandItem().getItem() == Items.GOLDEN_APPLE ||
                    mc.player.getOffhandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            if (grim) {
                speed = 1.0F;
            }
        } else {
            speed = 1.0F;
        }

        if (speed <= 0.2F) {
            Sprint.allow_sprint = false;
        }

        event.setSlow(speed);
    }

    @EventTarget
    private void onMotion(EventMotion event) {
        assert mc.player != null;

        if (mode.is("Grim")) {
            boolean grim = mc.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE ||
                    mc.player.getMainHandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE ||
                    mc.player.getOffhandItem().getItem() == Items.GOLDEN_APPLE ||
                    mc.player.getOffhandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE;

            if (event.isPre()) {
                if (mc.player.isUsingItem()) {
                }
            }
        }
    }
}
