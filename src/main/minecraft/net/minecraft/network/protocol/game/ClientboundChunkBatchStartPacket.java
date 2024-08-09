package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundChunkBatchStartPacket() implements Packet<ClientGamePacketListener> {
   public ClientboundChunkBatchStartPacket(FriendlyByteBuf pBuffer) {
      this();
   }

   public void write(FriendlyByteBuf pBuffer) {
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleChunkBatchStart(this);
   }
}