package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerLoginPacketListener extends ServerPacketListener {
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.LOGIN;
   }

   void handleHello(ServerboundHelloPacket pPacket);

   void handleKey(ServerboundKeyPacket pPacket);

   void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket pPacket);

   void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket pPacket);
}