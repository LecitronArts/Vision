package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldGenAttemptDebugPayload(BlockPos pos, float scale, float red, float green, float blue, float alpha) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/worldgen_attempt");

   public WorldGenAttemptDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readBlockPos(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
      pBuffer.writeFloat(this.scale);
      pBuffer.writeFloat(this.red);
      pBuffer.writeFloat(this.green);
      pBuffer.writeFloat(this.blue);
      pBuffer.writeFloat(this.alpha);
   }

   public ResourceLocation id() {
      return ID;
   }
}