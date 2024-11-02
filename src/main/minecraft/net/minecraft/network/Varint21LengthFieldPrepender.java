package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
   public static final int MAX_VARINT21_BYTES = 8;

   protected void encode(ChannelHandlerContext pContext, ByteBuf pEncoder, ByteBuf pDecoder) {
      int i = pEncoder.readableBytes();
      int j = VarInt.getByteSize(i);
      if (j > MAX_VARINT21_BYTES) {
         throw new EncoderException("unable to fit " + i + " into 8");
      } else {
         pDecoder.ensureWritable(j + i);
         VarInt.write(pDecoder, i);
         pDecoder.writeBytes(pEncoder, pEncoder.readerIndex(), i);
      }
   }
}