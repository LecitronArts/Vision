package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class AbstractCookingRecipe implements Recipe<Container> {
   protected final RecipeType<?> type;
   protected final CookingBookCategory category;
   protected final String group;
   protected final Ingredient ingredient;
   protected final ItemStack result;
   protected final float experience;
   protected final int cookingTime;

   public AbstractCookingRecipe(RecipeType<?> pType, String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime) {
      this.type = pType;
      this.category = pCategory;
      this.group = pGroup;
      this.ingredient = pIngredient;
      this.result = pResult;
      this.experience = pExperience;
      this.cookingTime = pCookingTime;
   }

   public boolean matches(Container pInv, Level pLevel) {
      return this.ingredient.test(pInv.getItem(0));
   }

   public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
      return this.result.copy();
   }

   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return true;
   }

   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> nonnulllist = NonNullList.create();
      nonnulllist.add(this.ingredient);
      return nonnulllist;
   }

   public float getExperience() {
      return this.experience;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      return this.result;
   }

   public String getGroup() {
      return this.group;
   }

   public int getCookingTime() {
      return this.cookingTime;
   }

   public RecipeType<?> getType() {
      return this.type;
   }

   public CookingBookCategory category() {
      return this.category;
   }

   public interface Factory<T extends AbstractCookingRecipe> {
      T create(String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime);
   }
}