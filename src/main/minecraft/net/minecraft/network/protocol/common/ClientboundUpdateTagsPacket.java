package net.minecraft.network.protocol.common;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;

public class ClientboundUpdateTagsPacket implements Packet<ClientCommonPacketListener> {
   private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags;

   public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> pTags) {
      this.tags = pTags;
   }

   public ClientboundUpdateTagsPacket(FriendlyByteBuf pBuffer) {
      this.tags = pBuffer.readMap(FriendlyByteBuf::readRegistryKey, TagNetworkSerialization.NetworkPayload::read);
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeMap(this.tags, FriendlyByteBuf::writeResourceKey, (p_297824_, p_298178_) -> {
         p_298178_.write(p_297824_);
      });
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handleUpdateTags(this);
   }

   public Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> getTags() {
      return this.tags;
   }
}