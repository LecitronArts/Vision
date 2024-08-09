package net.minecraft.network.protocol.common.custom;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.resources.ResourceLocation;

public record BreezeDebugPayload(BreezeDebugPayload.BreezeInfo breezeInfo) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/breeze");

   public BreezeDebugPayload(FriendlyByteBuf pBuffer) {
      this(new BreezeDebugPayload.BreezeInfo(pBuffer));
   }

   public void write(FriendlyByteBuf pBuffer) {
      this.breezeInfo.write(pBuffer);
   }

   public ResourceLocation id() {
      return ID;
   }

   public static record BreezeInfo(UUID uuid, int id, Integer attackTarget, BlockPos jumpTarget) {
      public BreezeInfo(FriendlyByteBuf pBuffer) {
         this(pBuffer.readUUID(), pBuffer.readInt(), pBuffer.readNullable(FriendlyByteBuf::readInt), pBuffer.readNullable(FriendlyByteBuf::readBlockPos));
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeUUID(this.uuid);
         pBuffer.writeInt(this.id);
         pBuffer.writeNullable(this.attackTarget, FriendlyByteBuf::writeInt);
         pBuffer.writeNullable(this.jumpTarget, FriendlyByteBuf::writeBlockPos);
      }

      public String generateName() {
         return DebugEntityNameGenerator.getEntityName(this.uuid);
      }

      public String toString() {
         return this.generateName();
      }
   }
}