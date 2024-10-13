package dev.vision.module.modules.combat;

import com.google.common.collect.Streams;
import dev.vision.events.EventMotion;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import dev.vision.module.values.BooleanValue;
import dev.vision.module.values.BoundsNumberValue;
import dev.vision.module.values.CheckBoxValue;
import dev.vision.module.values.ModeValue;
import dev.vision.util.timer.CPSUtil;
import dev.vision.util.timer.TimerUtil;
import me.empty.api.event.component.EventTarget;
import me.empty.japi.bounds.Bounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ModuleInfo(name = "KillAura", displayName = "Kill Aura", category = Category.Combat)
public class KillAura extends BasicModule {
    // ===========================VALUES============================
    private final ModeValue cpsPattern = new ModeValue("CPS Pattern", new String[] {"Basic", "Delay", "1.9+", "HurtTime"}, "Basic");
    private final BoundsNumberValue cpsBounds = new BoundsNumberValue("CPS Bounds", new Bounds(8, 13), 1, 20, 1, () -> cpsPattern.is("Basic"));
    private final BoundsNumberValue reduceCpsBounds = new BoundsNumberValue("Reduce CPS Bounds", new Bounds(6, 9), 1, 20, 1, () -> cpsPattern.is("Basic"));
    private final BoundsNumberValue delayBounds = new BoundsNumberValue("Delay Bounds", new Bounds(100, 200), 0, 1000, 10, () -> cpsPattern.is("Delay"));
    private final BoundsNumberValue reduceDelayBounds = new BoundsNumberValue("Reduce Delay Bounds", new Bounds(200, 300), 0, 1000, 10, () -> cpsPattern.is("Delay"));

    private final CheckBoxValue targets = new CheckBoxValue("Targets", new BooleanValue[] {
            new BooleanValue("Players", true),
            new BooleanValue("Mobs", false),
            new BooleanValue("Animals", false),
            new BooleanValue("Dead", true),
            new BooleanValue("ArmorStands", false),
    });

    private final ModeValue rotationMode = new ModeValue("Rotation Mode", new String[] {"Normal"}, "Normal");
    private final BooleanValue lockView = new BooleanValue("Lock view", false);

    // ===========================UTILS============================
    private final TimerUtil delayCps = new TimerUtil();
    private final CPSUtil cpsUtil = new CPSUtil();
    private LivingEntity target = null;


    @EventTarget
    private void onMotion(EventMotion event) {
        if (event.isPre()) {
            
        }
    }

    private void attack() {
        assert mc.player != null;
        if (target == null) {
            return;
        }

        boolean shouldClick;

        switch (cpsPattern.getValue()) {
            case "Delay" : {
                int cps = delayBounds.getValue().getRandom().intValue();
                if (target.hurtTime > 0) {
                    cps = reduceDelayBounds.getValue().getRandom().intValue();
                }
                shouldClick = delayCps.hasTimePassed(cps);
                break;
            }
            case "1.9+" : {
                shouldClick = mc.player.getAttackStrengthScale(mc.getDeltaFrameTime()) == 1.0F;
                break;
            }
            case "HurtTime" : {
                shouldClick = target.hurtTime == 0;
                break;
            }
            default : {
                int cps = cpsBounds.getValue().getRandom().intValue();
                if (target.hurtTime > 0) {
                    cps = reduceCpsBounds.getValue().getRandom().intValue();
                }
                shouldClick = cpsUtil.should(cps);
                break;
            }
        }

        if (shouldClick) {

            delayCps.reset();
            cpsUtil.reset();
        }
    }
}
