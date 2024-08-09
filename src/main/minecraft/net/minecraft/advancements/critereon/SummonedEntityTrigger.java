package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class SummonedEntityTrigger extends SimpleCriterionTrigger<SummonedEntityTrigger.TriggerInstance> {
   public Codec<SummonedEntityTrigger.TriggerInstance> codec() {
      return SummonedEntityTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_68265_) -> {
         return p_68265_.matches(lootcontext);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SummonedEntityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_308157_) -> {
         return p_308157_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(SummonedEntityTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "entity").forGetter(SummonedEntityTrigger.TriggerInstance::entity)).apply(p_308157_, SummonedEntityTrigger.TriggerInstance::new);
      });

      public static Criterion<SummonedEntityTrigger.TriggerInstance> summonedEntity(EntityPredicate.Builder pEntity) {
         return CriteriaTriggers.SUMMONED_ENTITY.createCriterion(new SummonedEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntity))));
      }

      public boolean matches(LootContext pLootContext) {
         return this.entity.isEmpty() || this.entity.get().matches(pLootContext);
      }

      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         pValidator.validateEntity(this.entity, ".entity");
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}