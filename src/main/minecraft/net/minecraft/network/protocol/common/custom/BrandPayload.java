package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record BrandPayload(String brand) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("brand");

   public BrandPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.brand);
   }

   public ResourceLocation id() {
      return ID;
   }
}