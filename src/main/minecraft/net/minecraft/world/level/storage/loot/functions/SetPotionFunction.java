package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction extends LootItemConditionalFunction {
   public static final Codec<SetPotionFunction> CODEC = RecordCodecBuilder.create((p_297172_) -> {
      return commonFields(p_297172_).and(BuiltInRegistries.POTION.holderByNameCodec().fieldOf("id").forGetter((p_297173_) -> {
         return p_297173_.potion;
      })).apply(p_297172_, SetPotionFunction::new);
   });
   private final Holder<Potion> potion;

   private SetPotionFunction(List<LootItemCondition> p_297236_, Holder<Potion> p_300134_) {
      super(p_297236_);
      this.potion = p_300134_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_POTION;
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      PotionUtils.setPotion(pStack, this.potion.value());
      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> setPotion(Potion pPotion) {
      return simpleBuilder((p_297171_) -> {
         return new SetPotionFunction(p_297171_, pPotion.builtInRegistryHolder());
      });
   }
}