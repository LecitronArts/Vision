package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundContainerSlotStateChangedPacket(int slotId, int containerId, boolean newState) implements Packet<ServerGamePacketListener> {
   public ServerboundContainerSlotStateChangedPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt(), pBuffer.readVarInt(), pBuffer.readBoolean());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.slotId);
      pBuffer.writeVarInt(this.containerId);
      pBuffer.writeBoolean(this.newState);
   }

   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleContainerSlotStateChanged(this);
   }
}