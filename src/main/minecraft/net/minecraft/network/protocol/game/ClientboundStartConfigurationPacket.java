package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundStartConfigurationPacket() implements Packet<ClientGamePacketListener> {
   public ClientboundStartConfigurationPacket(FriendlyByteBuf pBuffer) {
      this();
   }

   public void write(FriendlyByteBuf pBuffer) {
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleConfigurationStart(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}