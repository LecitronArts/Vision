package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiAddedDebugPayload(BlockPos pos, String type, int freeTicketCount) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_added");

   public PoiAddedDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readBlockPos(), pBuffer.readUtf(), pBuffer.readInt());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
      pBuffer.writeUtf(this.type);
      pBuffer.writeInt(this.freeTicketCount);
   }

   public ResourceLocation id() {
      return ID;
   }
}