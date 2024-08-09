package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record VillageSectionsDebugPayload(Set<SectionPos> villageChunks, Set<SectionPos> notVillageChunks) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/village_sections");

   public VillageSectionsDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readCollection(HashSet::new, FriendlyByteBuf::readSectionPos), pBuffer.readCollection(HashSet::new, FriendlyByteBuf::readSectionPos));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeCollection(this.villageChunks, FriendlyByteBuf::writeSectionPos);
      pBuffer.writeCollection(this.notVillageChunks, FriendlyByteBuf::writeSectionPos);
   }

   public ResourceLocation id() {
      return ID;
   }
}