package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;

public record ServerboundCustomQueryAnswerPacket(int transactionId, @Nullable CustomQueryAnswerPayload payload) implements Packet<ServerLoginPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 2097152;

   public static ServerboundCustomQueryAnswerPacket read(FriendlyByteBuf pBuffer) {
      int i = pBuffer.readVarInt();
      return new ServerboundCustomQueryAnswerPacket(i, readPayload(i, pBuffer));
   }

   private static CustomQueryAnswerPayload readPayload(int pTransactionId, FriendlyByteBuf pBuffer) {
      return readUnknownPayload(pBuffer);
   }

   private static CustomQueryAnswerPayload readUnknownPayload(FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
         pBuffer.skipBytes(i);
         return DiscardedQueryAnswerPayload.INSTANCE;
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 2097152 bytes");
      }
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.transactionId);
      pBuffer.writeNullable(this.payload, (p_300758_, p_298999_) -> {
         p_298999_.write(p_300758_);
      });
   }

   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleCustomQueryPacket(this);
   }
}