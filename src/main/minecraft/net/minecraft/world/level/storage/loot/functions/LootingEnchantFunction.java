package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class LootingEnchantFunction extends LootItemConditionalFunction {
   public static final int NO_LIMIT = 0;
   public static final Codec<LootingEnchantFunction> CODEC = RecordCodecBuilder.create((p_298785_) -> {
      return commonFields(p_298785_).and(p_298785_.group(NumberProviders.CODEC.fieldOf("count").forGetter((p_300767_) -> {
         return p_300767_.value;
      }), ExtraCodecs.strictOptionalField(Codec.INT, "limit", 0).forGetter((p_301305_) -> {
         return p_301305_.limit;
      }))).apply(p_298785_, LootingEnchantFunction::new);
   });
   private final NumberProvider value;
   private final int limit;

   LootingEnchantFunction(List<LootItemCondition> p_299292_, NumberProvider p_165227_, int p_165228_) {
      super(p_299292_);
      this.value = p_165227_;
      this.limit = p_165228_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.LOOTING_ENCHANT;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Sets.union(ImmutableSet.of(LootContextParams.KILLER_ENTITY), this.value.getReferencedContextParams());
   }

   private boolean hasLimit() {
      return this.limit > 0;
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      Entity entity = pContext.getParamOrNull(LootContextParams.KILLER_ENTITY);
      if (entity instanceof LivingEntity) {
         int i = EnchantmentHelper.getMobLooting((LivingEntity)entity);
         if (i == 0) {
            return pStack;
         }

         float f = (float)i * this.value.getFloat(pContext);
         pStack.grow(Math.round(f));
         if (this.hasLimit() && pStack.getCount() > this.limit) {
            pStack.setCount(this.limit);
         }
      }

      return pStack;
   }

   public static LootingEnchantFunction.Builder lootingMultiplier(NumberProvider pLootingMultiplier) {
      return new LootingEnchantFunction.Builder(pLootingMultiplier);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<LootingEnchantFunction.Builder> {
      private final NumberProvider count;
      private int limit = 0;

      public Builder(NumberProvider pLootingMultiplier) {
         this.count = pLootingMultiplier;
      }

      protected LootingEnchantFunction.Builder getThis() {
         return this;
      }

      public LootingEnchantFunction.Builder setLimit(int pLimit) {
         this.limit = pLimit;
         return this;
      }

      public LootItemFunction build() {
         return new LootingEnchantFunction(this.getConditions(), this.count, this.limit);
      }
   }
}