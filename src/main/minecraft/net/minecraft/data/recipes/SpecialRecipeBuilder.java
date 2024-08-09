package net.minecraft.data.recipes;

import java.util.function.Function;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

public class SpecialRecipeBuilder {
   private final Function<CraftingBookCategory, Recipe<?>> factory;

   public SpecialRecipeBuilder(Function<CraftingBookCategory, Recipe<?>> p_312302_) {
      this.factory = p_312302_;
   }

   public static SpecialRecipeBuilder special(Function<CraftingBookCategory, Recipe<?>> p_310896_) {
      return new SpecialRecipeBuilder(p_310896_);
   }

   public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
      this.save(pRecipeOutput, new ResourceLocation(pRecipeId));
   }

   public void save(RecipeOutput pRecipeOutput, ResourceLocation pRecipeId) {
      pRecipeOutput.accept(pRecipeId, this.factory.apply(CraftingBookCategory.MISC), (AdvancementHolder)null);
   }
}