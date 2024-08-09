package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HiveDebugPayload(HiveDebugPayload.HiveInfo hiveInfo) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/hive");

   public HiveDebugPayload(FriendlyByteBuf pBuffer) {
      this(new HiveDebugPayload.HiveInfo(pBuffer));
   }

   public void write(FriendlyByteBuf pBuffer) {
      this.hiveInfo.write(pBuffer);
   }

   public ResourceLocation id() {
      return ID;
   }

   public static record HiveInfo(BlockPos pos, String hiveType, int occupantCount, int honeyLevel, boolean sedated) {
      public HiveInfo(FriendlyByteBuf pBuffer) {
         this(pBuffer.readBlockPos(), pBuffer.readUtf(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readBoolean());
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeBlockPos(this.pos);
         pBuffer.writeUtf(this.hiveType);
         pBuffer.writeInt(this.occupantCount);
         pBuffer.writeInt(this.honeyLevel);
         pBuffer.writeBoolean(this.sedated);
      }
   }
}