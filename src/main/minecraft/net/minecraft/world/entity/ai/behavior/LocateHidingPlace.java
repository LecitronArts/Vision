package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class LocateHidingPlace {
   public static OneShot<LivingEntity> create(int pRadius, float pSpeedModifier, int pCloseEnoughDist) {
      return BehaviorBuilder.create((p_258505_) -> {
         return p_258505_.group(p_258505_.absent(MemoryModuleType.WALK_TARGET), p_258505_.registered(MemoryModuleType.HOME), p_258505_.registered(MemoryModuleType.HIDING_PLACE), p_258505_.registered(MemoryModuleType.PATH), p_258505_.registered(MemoryModuleType.LOOK_TARGET), p_258505_.registered(MemoryModuleType.BREED_TARGET), p_258505_.registered(MemoryModuleType.INTERACTION_TARGET)).apply(p_258505_, (p_258484_, p_258485_, p_258486_, p_258487_, p_258488_, p_258489_, p_258490_) -> {
            return (p_309061_, p_309062_, p_309063_) -> {
               p_309061_.getPoiManager().find((p_217258_) -> {
                  return p_217258_.is(PoiTypes.HOME);
               }, (p_23425_) -> {
                  return true;
               }, p_309062_.blockPosition(), pCloseEnoughDist + 1, PoiManager.Occupancy.ANY).filter((p_309077_) -> {
                  return p_309077_.closerToCenterThan(p_309062_.position(), (double)pCloseEnoughDist);
               }).or(() -> {
                  return p_309061_.getPoiManager().getRandom((p_217256_) -> {
                     return p_217256_.is(PoiTypes.HOME);
                  }, (p_23421_) -> {
                     return true;
                  }, PoiManager.Occupancy.ANY, p_309062_.blockPosition(), pRadius, p_309062_.getRandom());
               }).or(() -> {
                  return p_258505_.<GlobalPos>tryGet(p_258485_).map(GlobalPos::pos);
               }).ifPresent((p_309074_) -> {
                  p_258487_.erase();
                  p_258488_.erase();
                  p_258489_.erase();
                  p_258490_.erase();
                  p_258486_.set(GlobalPos.of(p_309061_.dimension(), p_309074_));
                  if (!p_309074_.closerToCenterThan(p_309062_.position(), (double)pCloseEnoughDist)) {
                     p_258484_.set(new WalkTarget(p_309074_, pSpeedModifier, pCloseEnoughDist));
                  }

               });
               return true;
            };
         });
      });
   }
}