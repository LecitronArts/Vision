package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShapelessRecipe implements CraftingRecipe {
   final String group;
   final CraftingBookCategory category;
   final ItemStack result;
   final NonNullList<Ingredient> ingredients;

   public ShapelessRecipe(String pGroup, CraftingBookCategory pCategory, ItemStack pResult, NonNullList<Ingredient> pIngredients) {
      this.group = pGroup;
      this.category = pCategory;
      this.result = pResult;
      this.ingredients = pIngredients;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPELESS_RECIPE;
   }

   public String getGroup() {
      return this.group;
   }

   public CraftingBookCategory category() {
      return this.category;
   }

   public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
      return this.result;
   }

   public NonNullList<Ingredient> getIngredients() {
      return this.ingredients;
   }

   public boolean matches(CraftingContainer pInv, Level pLevel) {
      StackedContents stackedcontents = new StackedContents();
      int i = 0;

      for(int j = 0; j < pInv.getContainerSize(); ++j) {
         ItemStack itemstack = pInv.getItem(j);
         if (!itemstack.isEmpty()) {
            ++i;
            stackedcontents.accountStack(itemstack, 1);
         }
      }

      return i == this.ingredients.size() && stackedcontents.canCraft(this, (IntList)null);
   }

   public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
      return this.result.copy();
   }

   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth * pHeight >= this.ingredients.size();
   }

   public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
      private static final Codec<ShapelessRecipe> CODEC = RecordCodecBuilder.create((p_309257_) -> {
         return p_309257_.group(ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter((p_299460_) -> {
            return p_299460_.group;
         }), CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((p_297437_) -> {
            return p_297437_.category;
         }), ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter((p_300770_) -> {
            return p_300770_.result;
         }), Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((p_297969_) -> {
            Ingredient[] aingredient = p_297969_.stream().filter((p_298915_) -> {
               return !p_298915_.isEmpty();
            }).toArray((p_298774_) -> {
               return new Ingredient[p_298774_];
            });
            if (aingredient.length == 0) {
               return DataResult.error(() -> {
                  return "No ingredients for shapeless recipe";
               });
            } else {
               return aingredient.length > 9 ? DataResult.error(() -> {
                  return "Too many ingredients for shapeless recipe";
               }) : DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
            }
         }, DataResult::success).forGetter((p_298509_) -> {
            return p_298509_.ingredients;
         })).apply(p_309257_, ShapelessRecipe::new);
      });

      public Codec<ShapelessRecipe> codec() {
         return CODEC;
      }

      public ShapelessRecipe fromNetwork(FriendlyByteBuf pBuffer) {
         String s = pBuffer.readUtf();
         CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
         int i = pBuffer.readVarInt();
         NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

         for(int j = 0; j < nonnulllist.size(); ++j) {
            nonnulllist.set(j, Ingredient.fromNetwork(pBuffer));
         }

         ItemStack itemstack = pBuffer.readItem();
         return new ShapelessRecipe(s, craftingbookcategory, itemstack, nonnulllist);
      }

      public void toNetwork(FriendlyByteBuf pBuffer, ShapelessRecipe pRecipe) {
         pBuffer.writeUtf(pRecipe.group);
         pBuffer.writeEnum(pRecipe.category);
         pBuffer.writeVarInt(pRecipe.ingredients.size());

         for(Ingredient ingredient : pRecipe.ingredients) {
            ingredient.toNetwork(pBuffer);
         }

         pBuffer.writeItem(pRecipe.result);
      }
   }
}