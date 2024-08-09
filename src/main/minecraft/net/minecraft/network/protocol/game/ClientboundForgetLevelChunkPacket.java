package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;

public record ClientboundForgetLevelChunkPacket(ChunkPos pos) implements Packet<ClientGamePacketListener> {
   public ClientboundForgetLevelChunkPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readChunkPos());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeChunkPos(this.pos);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleForgetLevelChunk(this);
   }
}