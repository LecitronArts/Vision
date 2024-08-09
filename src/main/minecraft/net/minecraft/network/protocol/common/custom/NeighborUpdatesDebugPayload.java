package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record NeighborUpdatesDebugPayload(long time, BlockPos pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/neighbors_update");

   public NeighborUpdatesDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarLong(), pBuffer.readBlockPos());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarLong(this.time);
      pBuffer.writeBlockPos(this.pos);
   }

   public ResourceLocation id() {
      return ID;
   }
}