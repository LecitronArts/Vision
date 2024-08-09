package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;

public record RecipeHolder<T extends Recipe<?>>(ResourceLocation id, T value) {
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof RecipeHolder) {
            RecipeHolder<?> recipeholder = (RecipeHolder)pOther;
            if (this.id.equals(recipeholder.id)) {
               return true;
            }
         }

         return false;
      }
   }

   public int hashCode() {
      return this.id.hashCode();
   }

   public String toString() {
      return this.id.toString();
   }
}