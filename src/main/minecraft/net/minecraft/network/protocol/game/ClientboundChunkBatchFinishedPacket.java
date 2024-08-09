package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundChunkBatchFinishedPacket(int batchSize) implements Packet<ClientGamePacketListener> {
   public ClientboundChunkBatchFinishedPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.batchSize);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleChunkBatchFinished(this);
   }
}