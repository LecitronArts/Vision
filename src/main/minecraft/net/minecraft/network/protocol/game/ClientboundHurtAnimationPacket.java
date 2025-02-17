package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.LivingEntity;

public record ClientboundHurtAnimationPacket(int id, float yaw) implements Packet<ClientGamePacketListener> {
   public ClientboundHurtAnimationPacket(LivingEntity pEntity) {
      this(pEntity.getId(), pEntity.getHurtDir());
   }

   public ClientboundHurtAnimationPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt(), pBuffer.readFloat());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.id);
      pBuffer.writeFloat(this.yaw);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleHurtAnimation(this);
   }
}