package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public class SimpleCraftingRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
   private final SimpleCraftingRecipeSerializer.Factory<T> constructor;
   private final Codec<T> codec;

   public SimpleCraftingRecipeSerializer(SimpleCraftingRecipeSerializer.Factory<T> pConstructor) {
      this.constructor = pConstructor;
      this.codec = RecordCodecBuilder.create((p_309259_) -> {
         return p_309259_.group(CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CraftingRecipe::category)).apply(p_309259_, pConstructor::create);
      });
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public T fromNetwork(FriendlyByteBuf pBuffer) {
      CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
      return this.constructor.create(craftingbookcategory);
   }

   public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
      pBuffer.writeEnum(pRecipe.category());
   }

   @FunctionalInterface
   public interface Factory<T extends CraftingRecipe> {
      T create(CraftingBookCategory pCategory);
   }
}