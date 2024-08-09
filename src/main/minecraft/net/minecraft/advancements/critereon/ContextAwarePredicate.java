package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;

public class ContextAwarePredicate {
   public static final Codec<ContextAwarePredicate> CODEC = LootItemConditions.CODEC.listOf().xmap(ContextAwarePredicate::new, (p_309450_) -> {
      return p_309450_.conditions;
   });
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositePredicates;

   ContextAwarePredicate(List<LootItemCondition> p_301186_) {
      this.conditions = p_301186_;
      this.compositePredicates = LootItemConditions.andConditions(p_301186_);
   }

   public static ContextAwarePredicate create(LootItemCondition... pConditions) {
      return new ContextAwarePredicate(List.of(pConditions));
   }

   public boolean matches(LootContext pContext) {
      return this.compositePredicates.test(pContext);
   }

   public void validate(ValidationContext pValidationContext) {
      for(int i = 0; i < this.conditions.size(); ++i) {
         LootItemCondition lootitemcondition = this.conditions.get(i);
         lootitemcondition.validate(pValidationContext.forChild("[" + i + "]"));
      }

   }
}