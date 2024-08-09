package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketDecoder extends ByteToMessageDecoder implements ProtocolSwapHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AttributeKey<ConnectionProtocol.CodecData<?>> codecKey;

   public PacketDecoder(AttributeKey<ConnectionProtocol.CodecData<?>> pCodecKey) {
      this.codecKey = pCodecKey;
   }

   protected void decode(ChannelHandlerContext pContext, ByteBuf pIn, List<Object> pOut) throws Exception {
      int i = pIn.readableBytes();
      if (i != 0) {
         Attribute<ConnectionProtocol.CodecData<?>> attribute = pContext.channel().attr(this.codecKey);
         ConnectionProtocol.CodecData<?> codecdata = attribute.get();
         FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(pIn);
         int j = friendlybytebuf.readVarInt();
         Packet<?> packet = codecdata.createPacket(j, friendlybytebuf);
         if (packet == null) {
            throw new IOException("Bad packet id " + j);
         } else {
            JvmProfiler.INSTANCE.onPacketReceived(codecdata.protocol(), j, pContext.channel().remoteAddress(), i);
            if (friendlybytebuf.readableBytes() > 0) {
               throw new IOException("Packet " + codecdata.protocol().id() + "/" + j + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + friendlybytebuf.readableBytes() + " bytes extra whilst reading packet " + j);
            } else {
               pOut.add(packet);
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug(Connection.PACKET_RECEIVED_MARKER, " IN: [{}:{}] {}", codecdata.protocol().id(), j, packet.getClass().getName());
               }

               ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
            }
         }
      }
   }
}