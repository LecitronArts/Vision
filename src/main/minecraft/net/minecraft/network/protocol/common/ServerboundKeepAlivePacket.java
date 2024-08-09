package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundKeepAlivePacket implements Packet<ServerCommonPacketListener> {
   private final long id;

   public ServerboundKeepAlivePacket(long pId) {
      this.id = pId;
   }

   public void handle(ServerCommonPacketListener pHandler) {
      pHandler.handleKeepAlive(this);
   }

   public ServerboundKeepAlivePacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readLong();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeLong(this.id);
   }

   public long getId() {
      return this.id;
   }
}