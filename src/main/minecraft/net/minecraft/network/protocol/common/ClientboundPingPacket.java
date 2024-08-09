package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPingPacket implements Packet<ClientCommonPacketListener> {
   private final int id;

   public ClientboundPingPacket(int pId) {
      this.id = pId;
   }

   public ClientboundPingPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readInt();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeInt(this.id);
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handlePing(this);
   }

   public int getId() {
      return this.id;
   }
}