package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;

public abstract class LootItemConditionalFunction implements LootItemFunction {
   protected final List<LootItemCondition> predicates;
   private final Predicate<LootContext> compositePredicates;

   protected LootItemConditionalFunction(List<LootItemCondition> pPredicates) {
      this.predicates = pPredicates;
      this.compositePredicates = LootItemConditions.andConditions(pPredicates);
   }

   protected static <T extends LootItemConditionalFunction> Products.P1<RecordCodecBuilder.Mu<T>, List<LootItemCondition>> commonFields(RecordCodecBuilder.Instance<T> pInstance) {
      return pInstance.group(ExtraCodecs.strictOptionalField(LootItemConditions.CODEC.listOf(), "conditions", List.of()).forGetter((p_297561_) -> {
         return p_297561_.predicates;
      }));
   }

   public final ItemStack apply(ItemStack pStack, LootContext pContext) {
      return this.compositePredicates.test(pContext) ? this.run(pStack, pContext) : pStack;
   }

   protected abstract ItemStack run(ItemStack pStack, LootContext pContext);

   public void validate(ValidationContext pContext) {
      LootItemFunction.super.validate(pContext);

      for(int i = 0; i < this.predicates.size(); ++i) {
         this.predicates.get(i).validate(pContext.forChild(".conditions[" + i + "]"));
      }

   }

   protected static LootItemConditionalFunction.Builder<?> simpleBuilder(Function<List<LootItemCondition>, LootItemFunction> pConstructor) {
      return new LootItemConditionalFunction.DummyBuilder(pConstructor);
   }

   public abstract static class Builder<T extends LootItemConditionalFunction.Builder<T>> implements LootItemFunction.Builder, ConditionUserBuilder<T> {
      private final ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

      public T when(LootItemCondition.Builder p_80694_) {
         this.conditions.add(p_80694_.build());
         return this.getThis();
      }

      public final T unwrap() {
         return this.getThis();
      }

      protected abstract T getThis();

      protected List<LootItemCondition> getConditions() {
         return this.conditions.build();
      }
   }

   static final class DummyBuilder extends LootItemConditionalFunction.Builder<LootItemConditionalFunction.DummyBuilder> {
      private final Function<List<LootItemCondition>, LootItemFunction> constructor;

      public DummyBuilder(Function<List<LootItemCondition>, LootItemFunction> pConstructor) {
         this.constructor = pConstructor;
      }

      protected LootItemConditionalFunction.DummyBuilder getThis() {
         return this;
      }

      public LootItemFunction build() {
         return this.constructor.apply(this.getConditions());
      }
   }
}