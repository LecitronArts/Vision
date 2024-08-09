package net.minecraft.client.multiplayer;

import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.LegacyProtocolUtils;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyServerPinger extends SimpleChannelInboundHandler<ByteBuf> {
   private static final Splitter SPLITTER = Splitter.on('\u0000').limit(6);
   private final ServerAddress address;
   private final LegacyServerPinger.Output output;

   public LegacyServerPinger(ServerAddress pAddress, LegacyServerPinger.Output pOutput) {
      this.address = pAddress;
      this.output = pOutput;
   }

   public void channelActive(ChannelHandlerContext pContext) throws Exception {
      super.channelActive(pContext);
      ByteBuf bytebuf = pContext.alloc().buffer();

      try {
         bytebuf.writeByte(254);
         bytebuf.writeByte(1);
         bytebuf.writeByte(250);
         LegacyProtocolUtils.writeLegacyString(bytebuf, "MC|PingHost");
         int i = bytebuf.writerIndex();
         bytebuf.writeShort(0);
         int j = bytebuf.writerIndex();
         bytebuf.writeByte(127);
         LegacyProtocolUtils.writeLegacyString(bytebuf, this.address.getHost());
         bytebuf.writeInt(this.address.getPort());
         int k = bytebuf.writerIndex() - j;
         bytebuf.setShort(i, k);
         pContext.channel().writeAndFlush(bytebuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      } catch (Exception exception) {
         bytebuf.release();
         throw exception;
      }
   }

   protected void channelRead0(ChannelHandlerContext pContext, ByteBuf pBuffer) {
      short short1 = pBuffer.readUnsignedByte();
      if (short1 == 255) {
         String s = LegacyProtocolUtils.readLegacyString(pBuffer);
         List<String> list = SPLITTER.splitToList(s);
         if ("\u00a71".equals(list.get(0))) {
            int i = Mth.getInt(list.get(1), 0);
            String s1 = list.get(2);
            String s2 = list.get(3);
            int j = Mth.getInt(list.get(4), -1);
            int k = Mth.getInt(list.get(5), -1);
            this.output.handleResponse(i, s1, s2, j, k);
         }
      }

      pContext.close();
   }

   public void exceptionCaught(ChannelHandlerContext pContext, Throwable pException) {
      pContext.close();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Output {
      void handleResponse(int p_297950_, String pVersion, String pMotd, int pPlayers, int pCapacity);
   }
}