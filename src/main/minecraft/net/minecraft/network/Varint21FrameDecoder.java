package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import javax.annotation.Nullable;

public class Varint21FrameDecoder extends ByteToMessageDecoder {
   private static final int MAX_VARINT21_BYTES = 8;
   private final ByteBuf helperBuf = Unpooled.directBuffer(3);
   @Nullable
   private final BandwidthDebugMonitor monitor;
   private final ByteBuf packetFixer$helperBuf = Unpooled.directBuffer(MAX_VARINT21_BYTES);

   public Varint21FrameDecoder(@Nullable BandwidthDebugMonitor pMonitor) {
      this.monitor = pMonitor;
   }

   protected void handlerRemoved0(ChannelHandlerContext pContext) {
      this.packetFixer$helperBuf.release();
   }

   private static boolean copyVarint(ByteBuf pIn, ByteBuf pOut) {
      for(int i = 0; i < MAX_VARINT21_BYTES; ++i) {
         if (!pIn.isReadable()) {
            return false;
         }

         byte b0 = pIn.readByte();
         pOut.writeByte(b0);
         if (!VarInt.hasContinuationBit(b0)) {
            return true;
         }
      }

      throw new CorruptedFrameException("length wider than 21-bit");
   }

   protected void decode(ChannelHandlerContext context, ByteBuf byteBuf, List<Object> list) {
      byteBuf.markReaderIndex();
      this.packetFixer$helperBuf.clear();
      if (!copyVarint(byteBuf, this.packetFixer$helperBuf)) {
         byteBuf.resetReaderIndex();
      } else {
         int i = VarInt.read(this.packetFixer$helperBuf);
         if (byteBuf.readableBytes() < i) {
            byteBuf.resetReaderIndex();
         } else {
            if (this.monitor != null) {
               this.monitor.onReceive(i + VarInt.getByteSize(i));
            }

            list.add(byteBuf.readBytes(i));
         }
      }
   }
}