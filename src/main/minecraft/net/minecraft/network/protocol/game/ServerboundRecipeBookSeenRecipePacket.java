package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ServerboundRecipeBookSeenRecipePacket implements Packet<ServerGamePacketListener> {
   private final ResourceLocation recipe;

   public ServerboundRecipeBookSeenRecipePacket(RecipeHolder<?> p_298515_) {
      this.recipe = p_298515_.id();
   }

   public ServerboundRecipeBookSeenRecipePacket(FriendlyByteBuf pBuffer) {
      this.recipe = pBuffer.readResourceLocation();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.recipe);
   }

   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleRecipeBookSeenRecipePacket(this);
   }

   public ResourceLocation getRecipe() {
      return this.recipe;
   }
}