package net.minecraft.network.protocol.game;

import java.time.Instant;
import javax.annotation.Nullable;

import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatPacket(String message, Instant timeStamp, long salt, @Nullable MessageSignature signature, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
   public ServerboundChatPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf(256), pBuffer.readInstant(), pBuffer.readLong(), pBuffer.readNullable(MessageSignature::read), new LastSeenMessages.Update(pBuffer));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.message,  ClientsideFixes.getChatLength());
      pBuffer.writeInstant(this.timeStamp);
      pBuffer.writeLong(this.salt);
      pBuffer.writeNullable(this.signature, MessageSignature::write);
      this.lastSeenMessages.write(pBuffer);
   }

   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleChat(this);
   }
}