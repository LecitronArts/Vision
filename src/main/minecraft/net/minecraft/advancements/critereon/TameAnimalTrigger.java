package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
   public Codec<TameAnimalTrigger.TriggerInstance> codec() {
      return TameAnimalTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, Animal pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_68838_) -> {
         return p_68838_.matches(lootcontext);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<TameAnimalTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_308158_) -> {
         return p_308158_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(TameAnimalTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "entity").forGetter(TameAnimalTrigger.TriggerInstance::entity)).apply(p_308158_, TameAnimalTrigger.TriggerInstance::new);
      });

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder pEntity) {
         return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntity))));
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