package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.common.custom.GameEventDebugPayload;
import net.minecraft.network.protocol.common.custom.GameEventListenerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiAddedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiRemovedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiTicketCountDebugPayload;
import net.minecraft.network.protocol.common.custom.RaidsDebugPayload;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.network.protocol.common.custom.VillageSectionsDebugPayload;
import net.minecraft.network.protocol.common.custom.WorldGenAttemptDebugPayload;
import net.minecraft.resources.ResourceLocation;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public record ClientboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ClientCommonPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;
   private static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder().put(BrandPayload.ID, BrandPayload::new).put(BeeDebugPayload.ID, BeeDebugPayload::new).put(BrainDebugPayload.ID, BrainDebugPayload::new).put(BreezeDebugPayload.ID, BreezeDebugPayload::new).put(GameEventDebugPayload.ID, GameEventDebugPayload::new).put(GameEventListenerDebugPayload.ID, GameEventListenerDebugPayload::new).put(GameTestAddMarkerDebugPayload.ID, GameTestAddMarkerDebugPayload::new).put(GameTestClearMarkersDebugPayload.ID, GameTestClearMarkersDebugPayload::new).put(GoalDebugPayload.ID, GoalDebugPayload::new).put(HiveDebugPayload.ID, HiveDebugPayload::new).put(NeighborUpdatesDebugPayload.ID, NeighborUpdatesDebugPayload::new).put(PathfindingDebugPayload.ID, PathfindingDebugPayload::new).put(PoiAddedDebugPayload.ID, PoiAddedDebugPayload::new).put(PoiRemovedDebugPayload.ID, PoiRemovedDebugPayload::new).put(PoiTicketCountDebugPayload.ID, PoiTicketCountDebugPayload::new).put(RaidsDebugPayload.ID, RaidsDebugPayload::new).put(StructuresDebugPayload.ID, StructuresDebugPayload::new).put(VillageSectionsDebugPayload.ID, VillageSectionsDebugPayload::new).put(WorldGenAttemptDebugPayload.ID, WorldGenAttemptDebugPayload::new).build();
   private static final Map<ResourceLocation, ProtocolVersion> viaFabricPlus$PAYLOAD_DIFF = ImmutableMap.<ResourceLocation, ProtocolVersion>builder()
           .put(BrandPayload.ID, LegacyProtocolVersion.c0_0_15a_1)
           .put(GameTestAddMarkerDebugPayload.ID, ProtocolVersion.v1_14)
           .put(GameTestClearMarkersDebugPayload.ID, ProtocolVersion.v1_14)
           .build();
   public ClientboundCustomPayloadPacket(FriendlyByteBuf pBuffer) {
      this(readPayload(pBuffer.readResourceLocation(), pBuffer));
   }

   private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      final ResourceLocation identifier = (ResourceLocation) pId;
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> viaFix;
      if (KNOWN_TYPES.containsKey(identifier)) {
         final FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(identifier);

         // Mods might add custom payloads that we don't want to filter, so we check for the namespace.
         // Mods should NEVER use the default namespace of the game, not only to not break this code,
         // but also to not break other mods and the game itself.
         if (!identifier.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            viaFix = reader;
         } else
            // Technically it's wrong to just drop all payloads, but ViaVersion doesn't translate them and the server can't detect if
            // we handled the payload or not, so dropping them is easier than adding a bunch of useless translations for payloads
            // which doesn't do anything on the client anyway.
            if (!viaFabricPlus$PAYLOAD_DIFF.containsKey(identifier) || ProtocolTranslator.getTargetVersion().olderThan(viaFabricPlus$PAYLOAD_DIFF.get(identifier))) {
               viaFix = null;
            } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
               // Skip remaining bytes after reading the payload and return null if the payload fails to read
               viaFix = (FriendlyByteBuf.Reader<? extends CustomPacketPayload>) packetByteBuf -> {
                  try {
                     final CustomPacketPayload result = reader.apply(packetByteBuf);
                     packetByteBuf.skipBytes(packetByteBuf.readableBytes());
                     return result;
                  } catch (Exception e) {
                     return null;
                  }
               };
            } else {
               viaFix = reader;
            }
      } else {
         viaFix = null;
      }
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = viaFix;
      return (CustomPacketPayload) (reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
   }

   private static DiscardedPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      int i = pBuffer.readableBytes();
      if (i >= 0 && i <= 1048576) {
         pBuffer.skipBytes(i);
         return new DiscardedPayload(pId);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.payload.id());
      this.payload.write(pBuffer);
   }

   public void handle(ClientCommonPacketListener pHandler) {
      pHandler.handleCustomPayload(this);
   }
}