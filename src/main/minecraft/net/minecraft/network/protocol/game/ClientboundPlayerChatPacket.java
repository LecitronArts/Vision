package net.minecraft.network.protocol.game;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatPacket(UUID sender, int index, @Nullable MessageSignature signature, SignedMessageBody.Packed body, @Nullable Component unsignedContent, FilterMask filterMask, ChatType.BoundNetwork chatType) implements Packet<ClientGamePacketListener> {
   public ClientboundPlayerChatPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUUID(), pBuffer.readVarInt(), pBuffer.readNullable(MessageSignature::read), new SignedMessageBody.Packed(pBuffer), pBuffer.readNullable(FriendlyByteBuf::readComponentTrusted), FilterMask.read(pBuffer), new ChatType.BoundNetwork(pBuffer));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUUID(this.sender);
      pBuffer.writeVarInt(this.index);
      pBuffer.writeNullable(this.signature, MessageSignature::write);
      this.body.write(pBuffer);
      pBuffer.writeNullable(this.unsignedContent, FriendlyByteBuf::writeComponent);
      FilterMask.write(pBuffer, this.filterMask);
      this.chatType.write(pBuffer);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlayerChat(this);
   }

   public boolean isSkippable() {
      return true;
   }
}