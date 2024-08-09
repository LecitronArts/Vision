package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
   public static final Codec<SetEnchantmentsFunction> CODEC = RecordCodecBuilder.create((p_297133_) -> {
      return commonFields(p_297133_).and(p_297133_.group(ExtraCodecs.strictOptionalField(Codec.unboundedMap(BuiltInRegistries.ENCHANTMENT.holderByNameCodec(), NumberProviders.CODEC), "enchantments", Map.of()).forGetter((p_297131_) -> {
         return p_297131_.enchantments;
      }), Codec.BOOL.fieldOf("add").orElse(false).forGetter((p_297132_) -> {
         return p_297132_.add;
      }))).apply(p_297133_, SetEnchantmentsFunction::new);
   });
   private final Map<Holder<Enchantment>, NumberProvider> enchantments;
   private final boolean add;

   SetEnchantmentsFunction(List<LootItemCondition> p_300544_, Map<Holder<Enchantment>, NumberProvider> p_165338_, boolean p_165339_) {
      super(p_300544_);
      this.enchantments = Map.copyOf(p_165338_);
      this.add = p_165339_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ENCHANTMENTS;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.enchantments.values().stream().flatMap((p_279081_) -> {
         return p_279081_.getReferencedContextParams().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      Object2IntMap<Enchantment> object2intmap = new Object2IntOpenHashMap<>();
      this.enchantments.forEach((p_297129_, p_297130_) -> {
         object2intmap.put(p_297129_.value(), p_297130_.getInt(pContext));
      });
      if (pStack.getItem() == Items.BOOK) {
         ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
         object2intmap.forEach((p_165343_, p_165344_) -> {
            EnchantedBookItem.addEnchantment(itemstack, new EnchantmentInstance(p_165343_, p_165344_));
         });
         return itemstack;
      } else {
         Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(pStack);
         if (this.add) {
            object2intmap.forEach((p_165366_, p_165367_) -> {
               updateEnchantment(map, p_165366_, Math.max(map.getOrDefault(p_165366_, 0) + p_165367_, 0));
            });
         } else {
            object2intmap.forEach((p_165361_, p_165362_) -> {
               updateEnchantment(map, p_165361_, Math.max(p_165362_, 0));
            });
         }

         EnchantmentHelper.setEnchantments(map, pStack);
         return pStack;
      }
   }

   private static void updateEnchantment(Map<Enchantment, Integer> pExistingEnchantments, Enchantment pEnchantment, int pLevel) {
      if (pLevel == 0) {
         pExistingEnchantments.remove(pEnchantment);
      } else {
         pExistingEnchantments.put(pEnchantment, pLevel);
      }

   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetEnchantmentsFunction.Builder> {
      private final ImmutableMap.Builder<Holder<Enchantment>, NumberProvider> enchantments = ImmutableMap.builder();
      private final boolean add;

      public Builder() {
         this(false);
      }

      public Builder(boolean pAdd) {
         this.add = pAdd;
      }

      protected SetEnchantmentsFunction.Builder getThis() {
         return this;
      }

      public SetEnchantmentsFunction.Builder withEnchantment(Enchantment pEnchantment, NumberProvider pLevelProvider) {
         this.enchantments.put(pEnchantment.builtInRegistryHolder(), pLevelProvider);
         return this;
      }

      public LootItemFunction build() {
         return new SetEnchantmentsFunction(this.getConditions(), this.enchantments.build(), this.add);
      }
   }
}