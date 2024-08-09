package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ClientboundPlaceGhostRecipePacket implements Packet<ClientGamePacketListener> {
   private final int containerId;
   private final ResourceLocation recipe;

   public ClientboundPlaceGhostRecipePacket(int pContainerId, RecipeHolder<?> pRecipe) {
      this.containerId = pContainerId;
      this.recipe = pRecipe.id();
   }

   public ClientboundPlaceGhostRecipePacket(FriendlyByteBuf pBuffer) {
      this.containerId = pBuffer.readByte();
      this.recipe = pBuffer.readResourceLocation();
   }

   public void write(FriendlyByteBuf pByteBuf) {
      pByteBuf.writeByte(this.containerId);
      pByteBuf.writeResourceLocation(this.recipe);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlaceRecipe(this);
   }

   public ResourceLocation getRecipe() {
      return this.recipe;
   }

   public int getContainerId() {
      return this.containerId;
   }
}