package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPongPacket implements Packet<ServerCommonPacketListener> {
   private final int id;

   public ServerboundPongPacket(int pId) {
      this.id = pId;
   }

   public ServerboundPongPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readInt();
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeInt(this.id);
   }

   public void handle(ServerCommonPacketListener pHandler) {
      pHandler.handlePong(this);
   }

   public int getId() {
      return this.id;
   }
}