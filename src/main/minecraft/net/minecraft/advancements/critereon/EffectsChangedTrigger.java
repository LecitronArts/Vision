package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
   public Codec<EffectsChangedTrigger.TriggerInstance> codec() {
      return EffectsChangedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, @Nullable Entity pSource) {
      LootContext lootcontext = pSource != null ? EntityPredicate.createContext(pPlayer, pSource) : null;
      this.trigger(pPlayer, (p_149268_) -> {
         return p_149268_.matches(pPlayer, lootcontext);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<MobEffectsPredicate> effects, Optional<ContextAwarePredicate> source) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EffectsChangedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_308120_) -> {
         return p_308120_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(EffectsChangedTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(MobEffectsPredicate.CODEC, "effects").forGetter(EffectsChangedTrigger.TriggerInstance::effects), ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "source").forGetter(EffectsChangedTrigger.TriggerInstance::source)).apply(p_308120_, EffectsChangedTrigger.TriggerInstance::new);
      });

      public static Criterion<EffectsChangedTrigger.TriggerInstance> hasEffects(MobEffectsPredicate.Builder pEffects) {
         return CriteriaTriggers.EFFECTS_CHANGED.createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), pEffects.build(), Optional.empty()));
      }

      public static Criterion<EffectsChangedTrigger.TriggerInstance> gotEffectsFrom(EntityPredicate.Builder pSource) {
         return CriteriaTriggers.EFFECTS_CHANGED.createCriterion(new EffectsChangedTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(pSource.build()))));
      }

      public boolean matches(ServerPlayer pPlayer, @Nullable LootContext pLootContext) {
         if (this.effects.isPresent() && !this.effects.get().matches(pPlayer)) {
            return false;
         } else {
            return !this.source.isPresent() || pLootContext != null && this.source.get().matches(pLootContext);
         }
      }

      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         pValidator.validateEntity(this.source, ".source");
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}