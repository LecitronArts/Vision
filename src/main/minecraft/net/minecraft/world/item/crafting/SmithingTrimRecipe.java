package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.Level;

public class SmithingTrimRecipe implements SmithingRecipe {
   final Ingredient template;
   final Ingredient base;
   final Ingredient addition;

   public SmithingTrimRecipe(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition) {
      this.template = pTemplate;
      this.base = pBase;
      this.addition = pAddition;
   }

   public boolean matches(Container pContainer, Level pLevel) {
      return this.template.test(pContainer.getItem(0)) && this.base.test(pContainer.getItem(1)) && this.addition.test(pContainer.getItem(2));
   }

   public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
      ItemStack itemstack = pContainer.getItem(1);
      if (this.base.test(itemstack)) {
         Optional<Holder.Reference<TrimMaterial>> optional = TrimMaterials.getFromIngredient(pRegistryAccess, pContainer.getItem(2));
         Optional<Holder.Reference<TrimPattern>> optional1 = TrimPatterns.getFromTemplate(pRegistryAccess, pContainer.getItem(0));
         if (optional.isPresent() && optional1.isPresent()) {
            Optional<ArmorTrim> optional2 = ArmorTrim.getTrim(pRegistryAccess, itemstack, false);
            if (optional2.isPresent() && optional2.get().hasPatternAndMaterial(optional1.get(), optional.get())) {
               return ItemStack.EMPTY;
            }

            ItemStack itemstack1 = itemstack.copy();
            itemstack1.setCount(1);
            ArmorTrim armortrim = new ArmorTrim(optional.get(), optional1.get());
            if (ArmorTrim.setTrim(pRegistryAccess, itemstack1, armortrim)) {
               return itemstack1;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      ItemStack itemstack = new ItemStack(Items.IRON_CHESTPLATE);
      Optional<Holder.Reference<TrimPattern>> optional = pRegistryAccess.registryOrThrow(Registries.TRIM_PATTERN).holders().findFirst();
      if (optional.isPresent()) {
         Optional<Holder.Reference<TrimMaterial>> optional1 = pRegistryAccess.registryOrThrow(Registries.TRIM_MATERIAL).getHolder(TrimMaterials.REDSTONE);
         if (optional1.isPresent()) {
            ArmorTrim armortrim = new ArmorTrim(optional1.get(), optional.get());
            ArmorTrim.setTrim(pRegistryAccess, itemstack, armortrim);
         }
      }

      return itemstack;
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
      return RecipeSerializer.SMITHING_TRIM;
   }

   public boolean isIncomplete() {
      return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
   }

   public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {
      private static final Codec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.create((p_297394_) -> {
         return p_297394_.group(Ingredient.CODEC.fieldOf("template").forGetter((p_298441_) -> {
            return p_298441_.template;
         }), Ingredient.CODEC.fieldOf("base").forGetter((p_297838_) -> {
            return p_297838_.base;
         }), Ingredient.CODEC.fieldOf("addition").forGetter((p_299309_) -> {
            return p_299309_.addition;
         })).apply(p_297394_, SmithingTrimRecipe::new);
      });

      public Codec<SmithingTrimRecipe> codec() {
         return CODEC;
      }

      public SmithingTrimRecipe fromNetwork(FriendlyByteBuf p_266888_) {
         Ingredient ingredient = Ingredient.fromNetwork(p_266888_);
         Ingredient ingredient1 = Ingredient.fromNetwork(p_266888_);
         Ingredient ingredient2 = Ingredient.fromNetwork(p_266888_);
         return new SmithingTrimRecipe(ingredient, ingredient1, ingredient2);
      }

      public void toNetwork(FriendlyByteBuf p_266901_, SmithingTrimRecipe p_266893_) {
         p_266893_.template.toNetwork(p_266901_);
         p_266893_.base.toNetwork(p_266901_);
         p_266893_.addition.toNetwork(p_266901_);
      }
   }
}