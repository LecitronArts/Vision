package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundFinishConfigurationPacket() implements Packet<ClientConfigurationPacketListener> {
   public ClientboundFinishConfigurationPacket(FriendlyByteBuf pBuffer) {
      this();
   }

   public void write(FriendlyByteBuf pBuffer) {
   }

   public void handle(ClientConfigurationPacketListener pHandler) {
      pHandler.handleConfigurationFinished(this);
   }

   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.PLAY;
   }
}