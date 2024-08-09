package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create((p_259429_) -> {
         return p_259429_.point((p_309107_, p_309108_, p_309109_) -> {
            p_309108_.getBrain().updateActivityFromSchedule(p_309107_.getDayTime(), p_309107_.getGameTime());
            return true;
         });
      });
   }
}