package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetScorePacket(String owner, String objectiveName, int score, @Nullable Component display, @Nullable NumberFormat numberFormat) implements Packet<ClientGamePacketListener> {
   public ClientboundSetScorePacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf(), pBuffer.readUtf(), pBuffer.readVarInt(), pBuffer.readNullable(FriendlyByteBuf::readComponentTrusted), pBuffer.readNullable(NumberFormatTypes::readFromStream));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.owner);
      pBuffer.writeUtf(this.objectiveName);
      pBuffer.writeVarInt(this.score);
      pBuffer.writeNullable(this.display, FriendlyByteBuf::writeComponent);
      pBuffer.writeNullable(this.numberFormat, NumberFormatTypes::writeToStream);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleSetScore(this);
   }
}