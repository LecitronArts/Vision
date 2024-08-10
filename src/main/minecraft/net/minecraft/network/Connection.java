package net.minecraft.network;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.type.EventState;
import com.google.common.base.Suppliers;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.Mth;
import net.minecraft.util.SampleLogger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Connection extends SimpleChannelInboundHandler<Packet<?>> {
   private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Marker ROOT_MARKER = MarkerFactory.getMarker("NETWORK");
   public static final Marker PACKET_MARKER = Util.make(MarkerFactory.getMarker("NETWORK_PACKETS"), (p_202569_) -> {
      p_202569_.add(ROOT_MARKER);
   });
   public static final Marker PACKET_RECEIVED_MARKER = Util.make(MarkerFactory.getMarker("PACKET_RECEIVED"), (p_202562_) -> {
      p_202562_.add(PACKET_MARKER);
   });
   public static final Marker PACKET_SENT_MARKER = Util.make(MarkerFactory.getMarker("PACKET_SENT"), (p_202557_) -> {
      p_202557_.add(PACKET_MARKER);
   });
   public static final AttributeKey<ConnectionProtocol.CodecData<?>> ATTRIBUTE_SERVERBOUND_PROTOCOL = AttributeKey.valueOf("serverbound_protocol");
   public static final AttributeKey<ConnectionProtocol.CodecData<?>> ATTRIBUTE_CLIENTBOUND_PROTOCOL = AttributeKey.valueOf("clientbound_protocol");
   public static final Supplier<NioEventLoopGroup> NETWORK_WORKER_GROUP = Suppliers.memoize(() -> {
      return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
   });
   public static final Supplier<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = Suppliers.memoize(() -> {
      return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
   });
   public static final Supplier<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = Suppliers.memoize(() -> {
      return new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
   });
   private final PacketFlow receiving;
   private final Queue<Consumer<Connection>> pendingActions = Queues.newConcurrentLinkedQueue();
   public Channel channel;
   private SocketAddress address;
   @Nullable
   private volatile PacketListener disconnectListener;
   @Nullable
   private volatile PacketListener packetListener;
   @Nullable
   private Component disconnectedReason;
   private boolean encrypted;
   private boolean disconnectionHandled;
   private int receivedPackets;
   private int sentPackets;
   private float averageReceivedPackets;
   private float averageSentPackets;
   private int tickCount;
   private boolean handlingFault;
   @Nullable
   private volatile Component delayedDisconnect;
   @Nullable
   BandwidthDebugMonitor bandwidthDebugMonitor;

   public Connection(PacketFlow pReceiving) {
      this.receiving = pReceiving;
   }

   public void channelActive(ChannelHandlerContext pContext) throws Exception {
      super.channelActive(pContext);
      this.channel = pContext.channel();
      this.address = this.channel.remoteAddress();
      if (this.delayedDisconnect != null) {
         this.disconnect(this.delayedDisconnect);
      }

   }

   public static void setInitialProtocolAttributes(Channel pChannel) {
      pChannel.attr(ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.HANDSHAKING.codec(PacketFlow.SERVERBOUND));
      pChannel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(ConnectionProtocol.HANDSHAKING.codec(PacketFlow.CLIENTBOUND));
   }

   public void channelInactive(ChannelHandlerContext pContext) {
      this.disconnect(Component.translatable("disconnect.endOfStream"));
   }

   public void exceptionCaught(ChannelHandlerContext pContext, Throwable pException) {
      if (pException instanceof SkipPacketException) {
         LOGGER.debug("Skipping packet due to errors", pException.getCause());
      } else {
         boolean flag = !this.handlingFault;
         this.handlingFault = true;
         if (this.channel.isOpen()) {
            if (pException instanceof TimeoutException) {
               LOGGER.debug("Timeout", pException);
               this.disconnect(Component.translatable("disconnect.timeout"));
            } else {
               Component component = Component.translatable("disconnect.genericReason", "Internal Exception: " + pException);
               if (flag) {
                  LOGGER.debug("Failed to sent packet", pException);
                  if (this.getSending() == PacketFlow.CLIENTBOUND) {
                     ConnectionProtocol connectionprotocol = this.channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).get().protocol();
                     Packet<?> packet = (Packet<?>)(connectionprotocol == ConnectionProtocol.LOGIN ? new ClientboundLoginDisconnectPacket(component) : new ClientboundDisconnectPacket(component));
                     this.send(packet, PacketSendListener.thenRun(() -> {
                        this.disconnect(component);
                     }));
                  } else {
                     this.disconnect(component);
                  }

                  this.setReadOnly();
               } else {
                  LOGGER.debug("Double fault", pException);
                  this.disconnect(component);
               }
            }

         }
      }
   }

   protected void channelRead0(ChannelHandlerContext pContext, Packet<?> pPacket) {
      if (this.channel.isOpen()) {
         PacketListener packetlistener = this.packetListener;
         if (packetlistener == null) {
            throw new IllegalStateException("Received a packet before the packet listener was initialized");
         } else {
            if (packetlistener.shouldHandleMessage(pPacket)) {
               try {
                  if (this.receiving == PacketFlow.CLIENTBOUND) {
                     for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
                        if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
                           ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((Connection) (Object) this, EventState.PRE, pPacket));
                        }
                     }
                  }
                  genericsFtw(pPacket, packetlistener);
               } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
               } catch (RejectedExecutionException rejectedexecutionexception) {
                  this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
               } catch (ClassCastException classcastexception) {
                  LOGGER.error("Received {} that couldn't be processed", pPacket.getClass(), classcastexception);
                  this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
               }

               ++this.receivedPackets;
            }

         }
      }
      if (this.channel.isOpen() || this.receiving == PacketFlow.CLIENTBOUND) {
         for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
               ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((Connection) (Object) this, EventState.POST, pPacket));
            }
         }
      }
   }

   private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
      pPacket.handle((T)pListener);
   }

   public void suspendInboundAfterProtocolChange() {
      this.channel.config().setAutoRead(false);
   }

   public void resumeInboundAfterProtocolChange() {
      this.channel.config().setAutoRead(true);
   }

   public void setListener(PacketListener pHandler) {
      Validate.notNull(pHandler, "packetListener");
      PacketFlow packetflow = pHandler.flow();
      if (packetflow != this.receiving) {
         throw new IllegalStateException("Trying to set listener for wrong side: connection is " + this.receiving + ", but listener is " + packetflow);
      } else {
         ConnectionProtocol connectionprotocol = pHandler.protocol();
         ConnectionProtocol connectionprotocol1 = this.channel.attr(getProtocolKey(packetflow)).get().protocol();
         if (connectionprotocol1 != connectionprotocol) {
            throw new IllegalStateException("Trying to set listener for protocol " + connectionprotocol.id() + ", but current " + packetflow + " protocol is " + connectionprotocol1.id());
         } else {
            this.packetListener = pHandler;
            this.disconnectListener = null;
         }
      }
   }

   public void setListenerForServerboundHandshake(PacketListener pPacketListener) {
      if (this.packetListener != null) {
         throw new IllegalStateException("Listener already set");
      } else if (this.receiving == PacketFlow.SERVERBOUND && pPacketListener.flow() == PacketFlow.SERVERBOUND && pPacketListener.protocol() == ConnectionProtocol.HANDSHAKING) {
         this.packetListener = pPacketListener;
      } else {
         throw new IllegalStateException("Invalid initial listener");
      }
   }

   public void initiateServerboundStatusConnection(String pHostName, int pPort, ClientStatusPacketListener pDisconnectListener) {
      this.initiateServerboundConnection(pHostName, pPort, pDisconnectListener, ClientIntent.STATUS);
   }

   public void initiateServerboundPlayConnection(String pHostName, int pPort, ClientLoginPacketListener pDisconnectListener) {
      this.initiateServerboundConnection(pHostName, pPort, pDisconnectListener, ClientIntent.LOGIN);
   }

   private void initiateServerboundConnection(String pHostName, int pPort, PacketListener pDisconnectListener, ClientIntent pIntention) {
      this.disconnectListener = pDisconnectListener;
      this.runOnceConnected((p_296374_) -> {
         p_296374_.setClientboundProtocolAfterHandshake(pIntention);
         this.setListener(pDisconnectListener);
         p_296374_.sendPacket(new ClientIntentionPacket(SharedConstants.getCurrentVersion().getProtocolVersion(), pHostName, pPort, pIntention), (PacketSendListener)null, true);
      });
   }

   public void setClientboundProtocolAfterHandshake(ClientIntent pIntention) {
      this.channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(pIntention.protocol().codec(PacketFlow.CLIENTBOUND));
   }

   public void send(Packet<?> pPacket) {
      this.send(pPacket, (PacketSendListener)null);
   }

   public void send(Packet<?> pPacket, @Nullable PacketSendListener pSendListener) {
      this.send(pPacket, pSendListener, true);
   }

   public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener, boolean pFlush) {
      if (this.isConnected()) {
         this.flushQueue();
         this.sendPacket(pPacket, pListener, pFlush);
      } else {
         this.pendingActions.add((p_296381_) -> {
            p_296381_.sendPacket(pPacket, pListener, pFlush);
         });
      }

   }

   public void runOnceConnected(Consumer<Connection> pAction) {
      if (this.isConnected()) {
         this.flushQueue();
         pAction.accept(this);
      } else {
         this.pendingActions.add(pAction);
      }

   }

   private void sendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
               ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((Connection) (Object) this, EventState.PRE, pPacket));
            }
         }
      }

      ++this.sentPackets;
      if (this.channel.eventLoop().inEventLoop()) {
         this.doSendPacket(pPacket, pSendListener, pFlush);
      } else {
         this.channel.eventLoop().execute(() -> {
            this.doSendPacket(pPacket, pSendListener, pFlush);
         });
      }
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == this) {
               ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((Connection) this, EventState.POST, pPacket));
            }
         }
      }

   }

   private void doSendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
      ChannelFuture channelfuture = pFlush ? this.channel.writeAndFlush(pPacket) : this.channel.write(pPacket);
      if (pSendListener != null) {
         channelfuture.addListener((p_243167_) -> {
            if (p_243167_.isSuccess()) {
               pSendListener.onSuccess();
            } else {
               Packet<?> packet = pSendListener.onFailure();
               if (packet != null) {
                  ChannelFuture channelfuture1 = this.channel.writeAndFlush(packet);
                  channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
               }
            }

         });
      }

      channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
   }

   public void flushChannel() {
      if (this.isConnected()) {
         this.flush();
      } else {
         this.pendingActions.add(Connection::flush);
      }

   }

   private void flush() {
      if (this.channel.eventLoop().inEventLoop()) {
         this.channel.flush();
      } else {
         this.channel.eventLoop().execute(() -> {
            this.channel.flush();
         });
      }

   }

   private static AttributeKey<ConnectionProtocol.CodecData<?>> getProtocolKey(PacketFlow pPacketFlow) {
      AttributeKey attributekey;
      switch (pPacketFlow) {
         case CLIENTBOUND:
            attributekey = ATTRIBUTE_CLIENTBOUND_PROTOCOL;
            break;
         case SERVERBOUND:
            attributekey = ATTRIBUTE_SERVERBOUND_PROTOCOL;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return attributekey;
   }

   private void flushQueue() {
      if (this.channel != null && this.channel.isOpen()) {
         synchronized(this.pendingActions) {
            Consumer<Connection> consumer;
            while((consumer = this.pendingActions.poll()) != null) {
               consumer.accept(this);
            }

         }
      }
   }

   public void tick() {
      this.flushQueue();
      PacketListener packetlistener = this.packetListener;
      if (packetlistener instanceof TickablePacketListener tickablepacketlistener) {
         tickablepacketlistener.tick();
      }

      if (!this.isConnected() && !this.disconnectionHandled) {
         this.handleDisconnection();
      }

      if (this.channel != null) {
         this.channel.flush();
      }

      if (this.tickCount++ % 20 == 0) {
         this.tickSecond();
      }

      if (this.bandwidthDebugMonitor != null) {
         this.bandwidthDebugMonitor.tick();
      }

   }

   protected void tickSecond() {
      this.averageSentPackets = Mth.lerp(0.75F, (float)this.sentPackets, this.averageSentPackets);
      this.averageReceivedPackets = Mth.lerp(0.75F, (float)this.receivedPackets, this.averageReceivedPackets);
      this.sentPackets = 0;
      this.receivedPackets = 0;
   }

   public SocketAddress getRemoteAddress() {
      return this.address;
   }

   public String getLoggableAddress(boolean pLogIps) {
      if (this.address == null) {
         return "local";
      } else {
         return pLogIps ? this.address.toString() : "IP hidden";
      }
   }

   public void disconnect(Component pMessage) {
      if (this.channel == null) {
         this.delayedDisconnect = pMessage;
      }

      if (this.isConnected()) {
         this.channel.close().awaitUninterruptibly();
         this.disconnectedReason = pMessage;
      }

   }

   public boolean isMemoryConnection() {
      return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
   }

   public PacketFlow getReceiving() {
      return this.receiving;
   }

   public PacketFlow getSending() {
      return this.receiving.getOpposite();
   }

   public static Connection connectToServer(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, @Nullable SampleLogger pBandwithLogger) {
      Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      if (pBandwithLogger != null) {
         connection.setBandwidthLogger(pBandwithLogger);
      }

      ChannelFuture channelfuture = connect(pAddress, pUseEpollIfAvailable, connection);
      channelfuture.syncUninterruptibly();
      return connection;
   }

   public static ChannelFuture connect(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, final Connection pConnection) {
      Class<? extends SocketChannel> oclass;
      EventLoopGroup eventloopgroup;
      if (Epoll.isAvailable() && pUseEpollIfAvailable) {
         oclass = EpollSocketChannel.class;
         eventloopgroup = NETWORK_EPOLL_WORKER_GROUP.get();
      } else {
         oclass = NioSocketChannel.class;
         eventloopgroup = NETWORK_WORKER_GROUP.get();
      }
      return (new Bootstrap()).group(eventloopgroup).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_129552_) {
            Connection.setInitialProtocolAttributes(p_129552_);

            try {
               p_129552_.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException channelexception) {
            }

            ChannelPipeline channelpipeline = p_129552_.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
            Connection.configureSerialization(channelpipeline, PacketFlow.CLIENTBOUND, pConnection.bandwidthDebugMonitor);
            pConnection.configurePacketHandler(channelpipeline);
         }
      }).channel(oclass).connect(pAddress.getAddress(), pAddress.getPort());
   }



   public static void configureSerialization(ChannelPipeline pPipeline, PacketFlow pFlow, @Nullable BandwidthDebugMonitor pBandwithMonitor) {
      PacketFlow packetflow = pFlow.getOpposite();
      AttributeKey<ConnectionProtocol.CodecData<?>> attributekey = getProtocolKey(pFlow);
      AttributeKey<ConnectionProtocol.CodecData<?>> attributekey1 = getProtocolKey(packetflow);
      pPipeline.addLast("splitter", new Varint21FrameDecoder(pBandwithMonitor)).addLast("decoder", new PacketDecoder(attributekey)).addLast("prepender", new Varint21LengthFieldPrepender()).addLast("encoder", new PacketEncoder(attributekey1)).addLast("unbundler", new PacketBundleUnpacker(attributekey1)).addLast("bundler", new PacketBundlePacker(attributekey));
   }

   public void configurePacketHandler(ChannelPipeline pPipeline) {
      pPipeline.addLast(new FlowControlHandler()).addLast("packet_handler", this);
   }

   private static void configureInMemoryPacketValidation(ChannelPipeline pPipeline, PacketFlow pFlow) {
      PacketFlow packetflow = pFlow.getOpposite();
      AttributeKey<ConnectionProtocol.CodecData<?>> attributekey = getProtocolKey(pFlow);
      AttributeKey<ConnectionProtocol.CodecData<?>> attributekey1 = getProtocolKey(packetflow);
      pPipeline.addLast("validator", new PacketFlowValidator(attributekey, attributekey1));
   }

   public static void configureInMemoryPipeline(ChannelPipeline pPipeline, PacketFlow pFlow) {
      configureInMemoryPacketValidation(pPipeline, pFlow);
   }

   public static Connection connectToLocalServer(SocketAddress pAddress) {
      final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      (new Bootstrap()).group(LOCAL_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_129557_) {
            Connection.setInitialProtocolAttributes(p_129557_);
            ChannelPipeline channelpipeline = p_129557_.pipeline();
            Connection.configureInMemoryPipeline(channelpipeline, PacketFlow.CLIENTBOUND);
            connection.configurePacketHandler(channelpipeline);
         }
      }).channel(LocalChannel.class).connect(pAddress).syncUninterruptibly();
      return connection;
   }

   public void setEncryptionKey(Cipher pDecryptingCipher, Cipher pEncryptingCipher) {
      this.encrypted = true;
      this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(pDecryptingCipher));
      this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(pEncryptingCipher));
   }

   public boolean isEncrypted() {
      return this.encrypted;
   }

   public boolean isConnected() {
      return this.channel != null && this.channel.isOpen();
   }

   public boolean isConnecting() {
      return this.channel == null;
   }

   @Nullable
   public PacketListener getPacketListener() {
      return this.packetListener;
   }

   @Nullable
   public Component getDisconnectedReason() {
      return this.disconnectedReason;
   }

   public void setReadOnly() {
      if (this.channel != null) {
         this.channel.config().setAutoRead(false);
      }

   }

   public void setupCompression(int pThreshold, boolean pValidateDecompressed) {
      if (pThreshold >= 0) {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
            ((CompressionDecoder)this.channel.pipeline().get("decompress")).setThreshold(pThreshold, pValidateDecompressed);
         } else {
            this.channel.pipeline().addBefore("decoder", "decompress", new CompressionDecoder(pThreshold, pValidateDecompressed));
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
            ((CompressionEncoder)this.channel.pipeline().get("compress")).setThreshold(pThreshold);
         } else {
            this.channel.pipeline().addBefore("encoder", "compress", new CompressionEncoder(pThreshold));
         }
      } else {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
            this.channel.pipeline().remove("decompress");
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
            this.channel.pipeline().remove("compress");
         }
      }

   }

   public void handleDisconnection() {
      if (this.channel != null && !this.channel.isOpen()) {
         if (this.disconnectionHandled) {
            LOGGER.warn("handleDisconnection() called twice");
         } else {
            this.disconnectionHandled = true;
            PacketListener packetlistener = this.getPacketListener();
            PacketListener packetlistener1 = packetlistener != null ? packetlistener : this.disconnectListener;
            if (packetlistener1 != null) {
               Component component = Objects.requireNonNullElseGet(this.getDisconnectedReason(), () -> {
                  return Component.translatable("multiplayer.disconnect.generic");
               });
               packetlistener1.onDisconnect(component);
            }

         }
      }
   }

   public float getAverageReceivedPackets() {
      return this.averageReceivedPackets;
   }

   public float getAverageSentPackets() {
      return this.averageSentPackets;
   }

   public void setBandwidthLogger(SampleLogger pBandwithLogger) {
      this.bandwidthDebugMonitor = new BandwidthDebugMonitor(pBandwithLogger);
   }
}