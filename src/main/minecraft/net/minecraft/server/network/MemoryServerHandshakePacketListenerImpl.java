package net.minecraft.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.server.MinecraftServer;

public class MemoryServerHandshakePacketListenerImpl implements ServerHandshakePacketListener {
   private final MinecraftServer server;
   private final Connection connection;

   public MemoryServerHandshakePacketListenerImpl(MinecraftServer pServer, Connection pConnection) {
      this.server = pServer;
      this.connection = pConnection;
   }

   public void handleIntention(ClientIntentionPacket pPacket) {
      if (pPacket.intention() != ClientIntent.LOGIN) {
         throw new UnsupportedOperationException("Invalid intention " + pPacket.intention());
      } else {
         this.connection.setClientboundProtocolAfterHandshake(ClientIntent.LOGIN);
         this.connection.setListener(new ServerLoginPacketListenerImpl(this.server, this.connection));
      }
   }

   public void onDisconnect(Component pReason) {
   }

   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }
}