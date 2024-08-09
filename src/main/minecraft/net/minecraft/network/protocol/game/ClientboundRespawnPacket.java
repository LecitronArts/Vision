package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundRespawnPacket(CommonPlayerSpawnInfo commonPlayerSpawnInfo, byte dataToKeep) implements Packet<ClientGamePacketListener> {
   public static final byte KEEP_ATTRIBUTES = 1;
   public static final byte KEEP_ENTITY_DATA = 2;
   public static final byte KEEP_ALL_DATA = 3;

   public ClientboundRespawnPacket(FriendlyByteBuf pBuffer) {
      this(new CommonPlayerSpawnInfo(pBuffer), pBuffer.readByte());
   }

   public void write(FriendlyByteBuf pBuffer) {
      this.commonPlayerSpawnInfo.write(pBuffer);
      pBuffer.writeByte(this.dataToKeep);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleRespawn(this);
   }

   public boolean shouldKeep(byte pData) {
      return (this.dataToKeep & pData) != 0;
   }
}