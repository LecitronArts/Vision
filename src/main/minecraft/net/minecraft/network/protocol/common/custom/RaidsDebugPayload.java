package net.minecraft.network.protocol.common.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RaidsDebugPayload(List<BlockPos> raidCenters) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/raids");

   public RaidsDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readList(FriendlyByteBuf::readBlockPos));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeCollection(this.raidCenters, FriendlyByteBuf::writeBlockPos);
   }

   public ResourceLocation id() {
      return ID;
   }
}