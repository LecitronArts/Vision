package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class StartRidingTrigger extends SimpleCriterionTrigger<StartRidingTrigger.TriggerInstance> {
   public Codec<StartRidingTrigger.TriggerInstance> codec() {
      return StartRidingTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer) {
      this.trigger(pPlayer, (p_160394_) -> {
         return true;
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<StartRidingTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_309601_) -> {
         return p_309601_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(StartRidingTrigger.TriggerInstance::player)).apply(p_309601_, StartRidingTrigger.TriggerInstance::new);
      });

      public static Criterion<StartRidingTrigger.TriggerInstance> playerStartsRiding(EntityPredicate.Builder pPlayer) {
         return CriteriaTriggers.START_RIDING_TRIGGER.createCriterion(new StartRidingTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer))));
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}