package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.util.SampleLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerStatusPinger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CANT_CONNECT_MESSAGE = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   private Connection setForcedVersion(InetSocketAddress address, boolean useEpoll, SampleLogger packetSizeLog, ServerData serverInfo) {

      if (serverInfo.viaFabricPlus$forcedVersion() != null && !serverInfo.viaFabricPlus$passedDirectConnectScreen()) {
         // We use the PerformanceLog field to store the forced version since it's always null when pinging a server
         // So we can create a dummy instance, store the forced version in it and later destroy the instance again
         // To avoid any side effects, we also support cases where a mod is also creating a PerformanceLog instance
         if (packetSizeLog == null) {
            packetSizeLog = new SampleLogger();
         }

         // Attach the forced version to the PerformanceLog instance
         ( packetSizeLog).viaFabricPlus$setForcedVersion(serverInfo.viaFabricPlus$forcedVersion());
         serverInfo.viaFabricPlus$passDirectConnectScreen(false);
      }

      return Connection.connectToServer(address, useEpoll, packetSizeLog);
   }
   public void pingServer(final ServerData pServer, final Runnable pServerListUpdater) throws UnknownHostException {
      final ServerAddress serveraddress =  ClientsideFixes.replaceDefaultPort(pServer.ip, pServer.viaFabricPlus$forcedVersion()) ;// ServerAddress.parseString(pServer.ip);

      Optional<InetSocketAddress> optional = ServerNameResolver.DEFAULT.resolveAddress(serveraddress).map(ResolvedServerAddress::asInetSocketAddress);
      if (optional.isEmpty()) {
         this.onPingFailed(ConnectScreen.UNKNOWN_HOST_MESSAGE, pServer);
      } else {
         final InetSocketAddress inetsocketaddress = optional.get();
         //redirect

         final Connection connection =  setForcedVersion(inetsocketaddress,false,(SampleLogger) null,pServer) ;// Connection.connectToServer(inetsocketaddress, false, (SampleLogger)null);

         this.connections.add(connection);
         pServer.motd = Component.translatable("multiplayer.status.pinging");
         pServer.ping = -1L;
         pServer.playerList = Collections.emptyList();
         ClientStatusPacketListener clientstatuspacketlistener = new ClientStatusPacketListener() {
            private boolean success;
            private boolean receivedPing;
            private long pingStart;

            public void handleStatusResponse(ClientboundStatusResponsePacket p_105489_) {

               ( pServer).viaFabricPlus$setTranslatingVersion(connection.viaFabricPlus$getTargetVersion());

               if (this.receivedPing) {
                  connection.disconnect(Component.translatable("multiplayer.status.unrequested"));
               } else {
                  this.receivedPing = true;
                  ServerStatus serverstatus = p_105489_.status();
                  pServer.motd = serverstatus.description();
                  serverstatus.version().ifPresentOrElse((p_273307_) -> {
                     pServer.version = Component.literal(p_273307_.name());
                     pServer.protocol = p_273307_.protocol();
                  }, () -> {
                     pServer.version = Component.translatable("multiplayer.status.old");
                     pServer.protocol = 0;
                  });
                  serverstatus.players().ifPresentOrElse((p_273230_) -> {
                     pServer.status = ServerStatusPinger.formatPlayerCount(p_273230_.online(), p_273230_.max());
                     pServer.players = p_273230_;
                     if (!p_273230_.sample().isEmpty()) {
                        List<Component> list = new ArrayList<>(p_273230_.sample().size());

                        for(GameProfile gameprofile : p_273230_.sample()) {
                           list.add(Component.literal(gameprofile.getName()));
                        }

                        if (p_273230_.sample().size() < p_273230_.online()) {
                           list.add(Component.translatable("multiplayer.status.and_more", p_273230_.online() - p_273230_.sample().size()));
                        }

                        pServer.playerList = list;
                     } else {
                        pServer.playerList = List.of();
                     }

                  }, () -> {
                     pServer.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY);
                  });
                  serverstatus.favicon().ifPresent((p_272704_) -> {
                     if (!Arrays.equals(p_272704_.iconBytes(), pServer.getIconBytes())) {
                        pServer.setIconBytes(ServerData.validateIcon(p_272704_.iconBytes()));
                        pServerListUpdater.run();
                     }

                  });
                  this.pingStart = Util.getMillis();
                  connection.send(new ServerboundPingRequestPacket(this.pingStart));
                  final ProtocolVersion version = (connection).viaFabricPlus$getTargetVersion();

                  // If the server is compatible with the client, we set the protocol version to the client version
                  if (version != null && version.getVersion() == pServer.protocol) {
                     pServer.protocol = SharedConstants.getProtocolVersion();
                  }
                  this.success = true;
               }
            }

            public void handlePongResponse(ClientboundPongResponsePacket p_105487_) {
               long i = this.pingStart;
               long j = Util.getMillis();
               pServer.ping = j - i;
               connection.disconnect(Component.translatable("multiplayer.status.finished"));
            }

            public void onDisconnect(Component p_105485_) {
               if (!this.success) {
                  ServerStatusPinger.this.onPingFailed(p_105485_, pServer);
                  ServerStatusPinger.this.pingLegacyServer(inetsocketaddress, serveraddress, pServer);
               }

            }

            public boolean isAcceptingMessages() {
               return connection.isConnected();
            }
         };

         try {
            connection.initiateServerboundStatusConnection(serveraddress.getHost(), serveraddress.getPort(), clientstatuspacketlistener);
            connection.send(new ServerboundStatusRequestPacket());
         } catch (Throwable throwable) {
            LOGGER.error("Failed to ping server {}", serveraddress, throwable);
         }

      }
   }

   void onPingFailed(Component pReason, ServerData pServerData) {
      LOGGER.error("Can't ping {}: {}", pServerData.ip, pReason.getString());
      pServerData.motd = CANT_CONNECT_MESSAGE;
      pServerData.status = CommonComponents.EMPTY;
   }

   /**
    * @author RK_01
    * @reason Remove legacy pingLegacyServer which didn't even work
    */
   void pingLegacyServer(InetSocketAddress pResolvedServerAddress, final ServerAddress pServerAddress, final ServerData pServerData) {
/*      (new Bootstrap()).group(Connection.NETWORK_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_105498_) {
            try {
               p_105498_.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException channelexception) {
            }

            p_105498_.pipeline().addLast(new LegacyServerPinger(pServerAddress, (p_298744_, p_300358_, p_297298_, p_299389_, p_297985_) -> {
               pServerData.protocol = -1;
               pServerData.version = Component.literal(p_300358_);
               pServerData.motd = Component.literal(p_297298_);
               pServerData.status = ServerStatusPinger.formatPlayerCount(p_299389_, p_297985_);
               pServerData.players = new ServerStatus.Players(p_297985_, p_299389_, List.of());
            }));
         }
      }).channel(NioSocketChannel.class).connect(pResolvedServerAddress.getAddress(), pResolvedServerAddress.getPort());*/
   }

   public static Component formatPlayerCount(int pPlayers, int pCapacity) {
      Component component = Component.literal(Integer.toString(pPlayers)).withStyle(ChatFormatting.GRAY);
      Component component1 = Component.literal(Integer.toString(pCapacity)).withStyle(ChatFormatting.GRAY);
      return Component.translatable("multiplayer.status.player_count", component, component1).withStyle(ChatFormatting.DARK_GRAY);
   }

   public void tick() {
      synchronized(this.connections) {
         Iterator<Connection> iterator = this.connections.iterator();

         while(iterator.hasNext()) {
            Connection connection = iterator.next();
            if (connection.isConnected()) {
               connection.tick();
            } else {
               iterator.remove();
               connection.handleDisconnection();
            }
         }

      }
   }

   public void removeAll() {
      synchronized(this.connections) {
         Iterator<Connection> iterator = this.connections.iterator();

         while(iterator.hasNext()) {
            Connection connection = iterator.next();
            if (connection.isConnected()) {
               iterator.remove();
               connection.disconnect(Component.translatable("multiplayer.status.cancelled"));
            }
         }

      }
   }
}