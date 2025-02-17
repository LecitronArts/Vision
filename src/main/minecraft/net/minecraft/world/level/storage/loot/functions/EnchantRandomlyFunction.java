package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<HolderSet<Enchantment>> ENCHANTMENT_SET_CODEC = BuiltInRegistries.ENCHANTMENT.holderByNameCodec().listOf().xmap(HolderSet::direct, (p_297089_) -> {
      return p_297089_.stream().toList();
   });
   public static final Codec<EnchantRandomlyFunction> CODEC = RecordCodecBuilder.create((p_297085_) -> {
      return commonFields(p_297085_).and(ExtraCodecs.strictOptionalField(ENCHANTMENT_SET_CODEC, "enchantments").forGetter((p_297084_) -> {
         return p_297084_.enchantments;
      })).apply(p_297085_, EnchantRandomlyFunction::new);
   });
   private final Optional<HolderSet<Enchantment>> enchantments;

   EnchantRandomlyFunction(List<LootItemCondition> p_298352_, Optional<HolderSet<Enchantment>> p_297532_) {
      super(p_298352_);
      this.enchantments = p_297532_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.ENCHANT_RANDOMLY;
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      RandomSource randomsource = pContext.getRandom();
      Optional<Holder<Enchantment>> optional = this.enchantments.flatMap((p_297095_) -> {
         return p_297095_.getRandomElement(randomsource);
      }).or(() -> {
         boolean flag = pStack.is(Items.BOOK);
         List<Holder.Reference<Enchantment>> list = BuiltInRegistries.ENCHANTMENT.holders().filter((p_297090_) -> {
            return p_297090_.value().isDiscoverable();
         }).filter((p_297093_) -> {
            return flag || p_297093_.value().canEnchant(pStack);
         }).toList();
         return Util.getRandomSafe(list, randomsource);
      });
      if (optional.isEmpty()) {
         LOGGER.warn("Couldn't find a compatible enchantment for {}", (Object)pStack);
         return pStack;
      } else {
         return enchantItem(pStack, optional.get().value(), randomsource);
      }
   }

   private static ItemStack enchantItem(ItemStack pStack, Enchantment pEnchantment, RandomSource pRandom) {
      int i = Mth.nextInt(pRandom, pEnchantment.getMinLevel(), pEnchantment.getMaxLevel());
      if (pStack.is(Items.BOOK)) {
         pStack = new ItemStack(Items.ENCHANTED_BOOK);
         EnchantedBookItem.addEnchantment(pStack, new EnchantmentInstance(pEnchantment, i));
      } else {
         pStack.enchant(pEnchantment, i);
      }

      return pStack;
   }

   public static EnchantRandomlyFunction.Builder randomEnchantment() {
      return new EnchantRandomlyFunction.Builder();
   }

   public static LootItemConditionalFunction.Builder<?> randomApplicableEnchantment() {
      return simpleBuilder((p_297086_) -> {
         return new EnchantRandomlyFunction(p_297086_, Optional.empty());
      });
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantRandomlyFunction.Builder> {
      private final List<Holder<Enchantment>> enchantments = new ArrayList<>();

      protected EnchantRandomlyFunction.Builder getThis() {
         return this;
      }

      public EnchantRandomlyFunction.Builder withEnchantment(Enchantment pEnchantment) {
         this.enchantments.add(pEnchantment.builtInRegistryHolder());
         return this;
      }

      public LootItemFunction build() {
         return new EnchantRandomlyFunction(this.getConditions(), this.enchantments.isEmpty() ? Optional.empty() : Optional.of(HolderSet.direct(this.enchantments)));
      }
   }
}