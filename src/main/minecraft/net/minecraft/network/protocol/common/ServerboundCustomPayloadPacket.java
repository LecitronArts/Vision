package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ServerCommonPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 65536;
   private static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder().put(BrandPayload.ID, BrandPayload::new).build();

   public ServerboundCustomPayloadPacket(FriendlyByteBuf pBuffer) {
      this(readPayload(pBuffer.readResourceLocation(), pBuffer));
   }

   private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(pId);
      return (CustomPacketPayload)(reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
   }

   private static DiscardedPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
         pBuffer.skipBytes(i);
         return new DiscardedPayload(pId);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
      }
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.payload.id());
      this.payload.write(pBuffer);
   }

   public void handle(ServerCommonPacketListener pHandler) {
      pHandler.handleCustomPayload(this);
   }
}