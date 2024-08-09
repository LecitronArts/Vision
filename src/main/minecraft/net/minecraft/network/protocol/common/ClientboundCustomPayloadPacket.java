package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
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

public record ClientboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ClientCommonPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;
   private static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder().put(BrandPayload.ID, BrandPayload::new).put(BeeDebugPayload.ID, BeeDebugPayload::new).put(BrainDebugPayload.ID, BrainDebugPayload::new).put(BreezeDebugPayload.ID, BreezeDebugPayload::new).put(GameEventDebugPayload.ID, GameEventDebugPayload::new).put(GameEventListenerDebugPayload.ID, GameEventListenerDebugPayload::new).put(GameTestAddMarkerDebugPayload.ID, GameTestAddMarkerDebugPayload::new).put(GameTestClearMarkersDebugPayload.ID, GameTestClearMarkersDebugPayload::new).put(GoalDebugPayload.ID, GoalDebugPayload::new).put(HiveDebugPayload.ID, HiveDebugPayload::new).put(NeighborUpdatesDebugPayload.ID, NeighborUpdatesDebugPayload::new).put(PathfindingDebugPayload.ID, PathfindingDebugPayload::new).put(PoiAddedDebugPayload.ID, PoiAddedDebugPayload::new).put(PoiRemovedDebugPayload.ID, PoiRemovedDebugPayload::new).put(PoiTicketCountDebugPayload.ID, PoiTicketCountDebugPayload::new).put(RaidsDebugPayload.ID, RaidsDebugPayload::new).put(StructuresDebugPayload.ID, StructuresDebugPayload::new).put(VillageSectionsDebugPayload.ID, VillageSectionsDebugPayload::new).put(WorldGenAttemptDebugPayload.ID, WorldGenAttemptDebugPayload::new).build();

   public ClientboundCustomPayloadPacket(FriendlyByteBuf pBuffer) {
      this(readPayload(pBuffer.readResourceLocation(), pBuffer));
   }

   private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
      FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(pId);
      return (CustomPacketPayload)(reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
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