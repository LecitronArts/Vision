package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerCommonPacketListener extends ServerPacketListener {
   void handleKeepAlive(ServerboundKeepAlivePacket pPacket);

   void handlePong(ServerboundPongPacket pPacket);

   void handleCustomPayload(ServerboundCustomPayloadPacket pPacket);

   void handleResourcePackResponse(ServerboundResourcePackPacket pPacket);

   void handleClientInformation(ServerboundClientInformationPacket pPacket);
}