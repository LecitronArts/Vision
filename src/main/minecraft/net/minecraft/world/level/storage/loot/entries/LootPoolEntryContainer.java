package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;

public abstract class LootPoolEntryContainer implements ComposableEntryContainer {
   protected final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositeCondition;

   protected LootPoolEntryContainer(List<LootItemCondition> pConditions) {
      this.conditions = pConditions;
      this.compositeCondition = LootItemConditions.andConditions(pConditions);
   }

   protected static <T extends LootPoolEntryContainer> Products.P1<RecordCodecBuilder.Mu<T>, List<LootItemCondition>> commonFields(RecordCodecBuilder.Instance<T> pInstance) {
      return pInstance.group(ExtraCodecs.strictOptionalField(LootItemConditions.CODEC.listOf(), "conditions", List.of()).forGetter((p_297410_) -> {
         return p_297410_.conditions;
      }));
   }

   public void validate(ValidationContext pValidationContext) {
      for(int i = 0; i < this.conditions.size(); ++i) {
         this.conditions.get(i).validate(pValidationContext.forChild(".condition[" + i + "]"));
      }

   }

   protected final boolean canRun(LootContext pLootContext) {
      return this.compositeCondition.test(pLootContext);
   }

   public abstract LootPoolEntryType getType();

   public abstract static class Builder<T extends LootPoolEntryContainer.Builder<T>> implements ConditionUserBuilder<T> {
      private final ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

      protected abstract T getThis();

      public T when(LootItemCondition.Builder pConditionBuilder) {
         this.conditions.add(pConditionBuilder.build());
         return this.getThis();
      }

      public final T unwrap() {
         return this.getThis();
      }

      protected List<LootItemCondition> getConditions() {
         return this.conditions.build();
      }

      public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         return new AlternativesEntry.Builder(this, pChildBuilder);
      }

      public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         return new EntryGroup.Builder(this, pChildBuilder);
      }

      public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         return new SequentialEntry.Builder(this, pChildBuilder);
      }

      public abstract LootPoolEntryContainer build();
   }
}