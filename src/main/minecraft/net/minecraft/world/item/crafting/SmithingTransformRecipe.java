package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SmithingTransformRecipe implements SmithingRecipe {
   final Ingredient template;
   final Ingredient base;
   final Ingredient addition;
   final ItemStack result;

   public SmithingTransformRecipe(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, ItemStack pResult) {
      this.template = pTemplate;
      this.base = pBase;
      this.addition = pAddition;
      this.result = pResult;
   }

   public boolean matches(Container pContainer, Level pLevel) {
      return this.template.test(pContainer.getItem(0)) && this.base.test(pContainer.getItem(1)) && this.addition.test(pContainer.getItem(2));
   }

   public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
      ItemStack itemstack = this.result.copy();
      CompoundTag compoundtag = pContainer.getItem(1).getTag();
      if (compoundtag != null) {
         itemstack.setTag(compoundtag.copy());
      }

      return itemstack;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      return this.result;
   }

   public boolean isTemplateIngredient(ItemStack pStack) {
      return this.template.test(pStack);
   }

   public boolean isBaseIngredient(ItemStack pStack) {
      return this.base.test(pStack);
   }

   public boolean isAdditionIngredient(ItemStack pStack) {
      return this.addition.test(pStack);
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SMITHING_TRANSFORM;
   }

   public boolean isIncomplete() {
      return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
   }

   public static class Serializer implements RecipeSerializer<SmithingTransformRecipe> {
      private static final Codec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.create((p_309262_) -> {
         return p_309262_.group(Ingredient.CODEC.fieldOf("template").forGetter((p_297231_) -> {
            return p_297231_.template;
         }), Ingredient.CODEC.fieldOf("base").forGetter((p_298250_) -> {
            return p_298250_.base;
         }), Ingredient.CODEC.fieldOf("addition").forGetter((p_299654_) -> {
            return p_299654_.addition;
         }), ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter((p_297480_) -> {
            return p_297480_.result;
         })).apply(p_309262_, SmithingTransformRecipe::new);
      });

      public Codec<SmithingTransformRecipe> codec() {
         return CODEC;
      }

      public SmithingTransformRecipe fromNetwork(FriendlyByteBuf p_267316_) {
         Ingredient ingredient = Ingredient.fromNetwork(p_267316_);
         Ingredient ingredient1 = Ingredient.fromNetwork(p_267316_);
         Ingredient ingredient2 = Ingredient.fromNetwork(p_267316_);
         ItemStack itemstack = p_267316_.readItem();
         return new SmithingTransformRecipe(ingredient, ingredient1, ingredient2, itemstack);
      }

      public void toNetwork(FriendlyByteBuf p_266746_, SmithingTransformRecipe p_266927_) {
         p_266927_.template.toNetwork(p_266746_);
         p_266927_.base.toNetwork(p_266746_);
         p_266927_.addition.toNetwork(p_266746_);
         p_266746_.writeItem(p_266927_.result);
      }
   }
}