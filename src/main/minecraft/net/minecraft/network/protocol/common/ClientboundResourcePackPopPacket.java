package net.minecraft.network.protocol.common;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundResourcePackPopPacket(Optional<UUID> id) implements Packet<ClientCommonPacketListener> {
   public ClientboundResourcePackPopPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readOptional(FriendlyByteBuf::readUUID));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeOptional(this.id, FriendlyByteBuf::writeUUID);
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handleResourcePackPop(this);
   }
}