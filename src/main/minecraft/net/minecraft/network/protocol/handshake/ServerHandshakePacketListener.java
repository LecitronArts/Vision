package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerHandshakePacketListener extends ServerPacketListener {
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.HANDSHAKING;
   }

   void handleIntention(ClientIntentionPacket pPacket);
}