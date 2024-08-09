package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiTicketCountDebugPayload(BlockPos pos, int freeTicketCount) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_ticket_count");

   public PoiTicketCountDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readBlockPos(), pBuffer.readInt());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
      pBuffer.writeInt(this.freeTicketCount);
   }

   public ResourceLocation id() {
      return ID;
   }
}