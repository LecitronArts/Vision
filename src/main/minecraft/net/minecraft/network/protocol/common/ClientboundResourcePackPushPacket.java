package net.minecraft.network.protocol.common;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public record ClientboundResourcePackPushPacket(UUID id, String url, String hash, boolean required, @Nullable Component prompt) implements Packet<ClientCommonPacketListener> {
   public static final int MAX_HASH_LENGTH = 40;

   public ClientboundResourcePackPushPacket {
      if (hash.length() > 40) {
         throw new IllegalArgumentException("Hash is too long (max 40, was " + hash.length() + ")");
      }
   }

   public ClientboundResourcePackPushPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUUID(), pBuffer.readUtf(), pBuffer.readUtf(40), pBuffer.readBoolean(), pBuffer.readNullable(FriendlyByteBuf::readComponentTrusted));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUUID(this.id);
      pBuffer.writeUtf(this.url);
      pBuffer.writeUtf(this.hash);
      pBuffer.writeBoolean(this.required);
      pBuffer.writeNullable(this.prompt, FriendlyByteBuf::writeComponent);
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handleResourcePackPush(this);
   }
}