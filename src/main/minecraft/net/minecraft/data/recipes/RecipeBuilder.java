package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.ItemLike;

public interface RecipeBuilder {
   ResourceLocation ROOT_RECIPE_ADVANCEMENT = new ResourceLocation("recipes/root");

   RecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion);

   RecipeBuilder group(@Nullable String pGroupName);

   Item getResult();

   void save(RecipeOutput pRecipeOutput, ResourceLocation pId);

   default void save(RecipeOutput pRecipeOutput) {
      this.save(pRecipeOutput, getDefaultRecipeId(this.getResult()));
   }

   default void save(RecipeOutput pRecipeOutput, String pId) {
      ResourceLocation resourcelocation = getDefaultRecipeId(this.getResult());
      ResourceLocation resourcelocation1 = new ResourceLocation(pId);
      if (resourcelocation1.equals(resourcelocation)) {
         throw new IllegalStateException("Recipe " + pId + " should remove its 'save' argument as it is equal to default one");
      } else {
         this.save(pRecipeOutput, resourcelocation1);
      }
   }

   static ResourceLocation getDefaultRecipeId(ItemLike pItemLike) {
      return BuiltInRegistries.ITEM.getKey(pItemLike.asItem());
   }

   static CraftingBookCategory determineBookCategory(RecipeCategory pCategory) {
      CraftingBookCategory craftingbookcategory;
      switch (pCategory) {
         case BUILDING_BLOCKS:
            craftingbookcategory = CraftingBookCategory.BUILDING;
            break;
         case TOOLS:
         case COMBAT:
            craftingbookcategory = CraftingBookCategory.EQUIPMENT;
            break;
         case REDSTONE:
            craftingbookcategory = CraftingBookCategory.REDSTONE;
            break;
         default:
            craftingbookcategory = CraftingBookCategory.MISC;
      }

      return craftingbookcategory;
   }
}