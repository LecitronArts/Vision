package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiRemovedDebugPayload(BlockPos pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_removed");

   public PoiRemovedDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readBlockPos());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
   }

   public ResourceLocation id() {
      return ID;
   }
}