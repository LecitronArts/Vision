package net.minecraft.advancements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AdvancementHolder(ResourceLocation id, Advancement value) {
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.id);
      this.value.write(pBuffer);
   }

   public static AdvancementHolder read(FriendlyByteBuf pBuffer) {
      return new AdvancementHolder(pBuffer.readResourceLocation(), Advancement.read(pBuffer));
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof AdvancementHolder) {
            AdvancementHolder advancementholder = (AdvancementHolder)pOther;
            if (this.id.equals(advancementholder.id)) {
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