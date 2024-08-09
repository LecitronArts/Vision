package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record BonusLevelTableCondition(Holder<Enchantment> enchantment, List<Float> values) implements LootItemCondition {
   public static final Codec<BonusLevelTableCondition> CODEC = RecordCodecBuilder.create((p_297182_) -> {
      return p_297182_.group(BuiltInRegistries.ENCHANTMENT.holderByNameCodec().fieldOf("enchantment").forGetter(BonusLevelTableCondition::enchantment), Codec.FLOAT.listOf().fieldOf("chances").forGetter(BonusLevelTableCondition::values)).apply(p_297182_, BonusLevelTableCondition::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.TABLE_BONUS;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.TOOL);
   }

   public boolean test(LootContext pContext) {
      ItemStack itemstack = pContext.getParamOrNull(LootContextParams.TOOL);
      int i = itemstack != null ? EnchantmentHelper.getItemEnchantmentLevel(this.enchantment.value(), itemstack) : 0;
      float f = this.values.get(Math.min(i, this.values.size() - 1));
      return pContext.getRandom().nextFloat() < f;
   }

   public static LootItemCondition.Builder bonusLevelFlatChance(Enchantment pEnchantment, float... pChances) {
      List<Float> list = new ArrayList<>(pChances.length);

      for(float f : pChances) {
         list.add(f);
      }

      return () -> {
         return new BonusLevelTableCondition(pEnchantment.builtInRegistryHolder(), list);
      };
   }
}