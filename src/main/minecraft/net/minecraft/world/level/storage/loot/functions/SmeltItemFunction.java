package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SmeltItemFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<SmeltItemFunction> CODEC = RecordCodecBuilder.create((p_298512_) -> {
      return commonFields(p_298512_).apply(p_298512_, SmeltItemFunction::new);
   });

   private SmeltItemFunction(List<LootItemCondition> p_298857_) {
      super(p_298857_);
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.FURNACE_SMELT;
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (pStack.isEmpty()) {
         return pStack;
      } else {
         Optional<RecipeHolder<SmeltingRecipe>> optional = pContext.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(pStack), pContext.getLevel());
         if (optional.isPresent()) {
            ItemStack itemstack = optional.get().value().getResultItem(pContext.getLevel().registryAccess());
            if (!itemstack.isEmpty()) {
               return itemstack.copyWithCount(pStack.getCount());
            }
         }

         LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", (Object)pStack);
         return pStack;
      }
   }

   public static LootItemConditionalFunction.Builder<?> smelted() {
      return simpleBuilder(SmeltItemFunction::new);
   }
}