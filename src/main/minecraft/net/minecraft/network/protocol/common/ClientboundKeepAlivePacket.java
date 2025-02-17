package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundKeepAlivePacket implements Packet<ClientCommonPacketListener> {
   private final long id;

   public ClientboundKeepAlivePacket(long pId) {
      this.id = pId;
   }

   public ClientboundKeepAlivePacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readLong();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeLong(this.id);
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handleKeepAlive(this);
   }

   public long getId() {
      return this.id;
   }
}