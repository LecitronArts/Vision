package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create((p_259349_) -> {
         return p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME)).apply(p_259349_, (p_259472_) -> {
            return (p_309089_, p_309090_, p_309091_) -> {
               Raid raid = p_309089_.getRaidAt(p_309090_.blockPosition());
               if (raid == null) {
                  p_309090_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
               }

               return true;
            };
         });
      });
   }
}