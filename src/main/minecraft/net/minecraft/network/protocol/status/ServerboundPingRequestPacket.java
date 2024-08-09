package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerPingPacketListener;

public class ServerboundPingRequestPacket implements Packet<ServerPingPacketListener> {
   private final long time;

   public ServerboundPingRequestPacket(long pTime) {
      this.time = pTime;
   }

   public ServerboundPingRequestPacket(FriendlyByteBuf pBuffer) {
      this.time = pBuffer.readLong();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeLong(this.time);
   }

   public void handle(ServerPingPacketListener pHandler) {
      pHandler.handlePingRequest(this);
   }

   public long getTime() {
      return this.time;
   }
}