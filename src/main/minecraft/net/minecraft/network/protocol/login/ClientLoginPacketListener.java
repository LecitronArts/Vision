package net.minecraft.network.protocol.login;

import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;

public interface ClientLoginPacketListener extends ClientboundPacketListener {
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.LOGIN;
   }

   void handleHello(ClientboundHelloPacket pPacket);

   void handleGameProfile(ClientboundGameProfilePacket pPacket);

   void handleDisconnect(ClientboundLoginDisconnectPacket pPacket);

   void handleCompression(ClientboundLoginCompressionPacket pPacket);

   void handleCustomQuery(ClientboundCustomQueryPacket pPacket);
}