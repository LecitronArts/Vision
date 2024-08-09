package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class ConstructBeaconTrigger extends SimpleCriterionTrigger<ConstructBeaconTrigger.TriggerInstance> {
   public Codec<ConstructBeaconTrigger.TriggerInstance> codec() {
      return ConstructBeaconTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, int pLevel) {
      this.trigger(pPlayer, (p_148028_) -> {
         return p_148028_.matches(pLevel);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints level) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ConstructBeaconTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_312562_) -> {
         return p_312562_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(ConstructBeaconTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "level", MinMaxBounds.Ints.ANY).forGetter(ConstructBeaconTrigger.TriggerInstance::level)).apply(p_312562_, ConstructBeaconTrigger.TriggerInstance::new);
      });

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon() {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon(MinMaxBounds.Ints pLevel) {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), pLevel));
      }

      public boolean matches(int pLevel) {
         return this.level.matches(pLevel);
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}