package net.minecraft.util.profiling.jfr.stats;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public final class NetworkPacketSummary {
   private final NetworkPacketSummary.PacketCountAndSize totalPacketCountAndSize;
   private final List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> largestSizeContributors;
   private final Duration recordingDuration;

   public NetworkPacketSummary(Duration pRecordingDuration, List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> pPacketInfos) {
      this.recordingDuration = pRecordingDuration;
      this.totalPacketCountAndSize = pPacketInfos.stream().map(Pair::getSecond).reduce(NetworkPacketSummary.PacketCountAndSize::add).orElseGet(() -> {
         return new NetworkPacketSummary.PacketCountAndSize(0L, 0L);
      });
      this.largestSizeContributors = pPacketInfos.stream().sorted(Comparator.comparing(Pair::getSecond, NetworkPacketSummary.PacketCountAndSize.SIZE_THEN_COUNT)).limit(10L).toList();
   }

   public double getCountsPerSecond() {
      return (double)this.totalPacketCountAndSize.totalCount / (double)this.recordingDuration.getSeconds();
   }

   public double getSizePerSecond() {
      return (double)this.totalPacketCountAndSize.totalSize / (double)this.recordingDuration.getSeconds();
   }

   public long getTotalCount() {
      return this.totalPacketCountAndSize.totalCount;
   }

   public long getTotalSize() {
      return this.totalPacketCountAndSize.totalSize;
   }

   public List<Pair<NetworkPacketSummary.PacketIdentification, NetworkPacketSummary.PacketCountAndSize>> largestSizeContributors() {
      return this.largestSizeContributors;
   }

   public static record PacketCountAndSize(long totalCount, long totalSize) {
      static final Comparator<NetworkPacketSummary.PacketCountAndSize> SIZE_THEN_COUNT = Comparator.comparing(NetworkPacketSummary.PacketCountAndSize::totalSize).thenComparing(NetworkPacketSummary.PacketCountAndSize::totalCount).reversed();

      NetworkPacketSummary.PacketCountAndSize add(NetworkPacketSummary.PacketCountAndSize pPacketCountAndSize) {
         return new NetworkPacketSummary.PacketCountAndSize(this.totalCount + pPacketCountAndSize.totalCount, this.totalSize + pPacketCountAndSize.totalSize);
      }
   }

   public static record PacketIdentification(PacketFlow direction, String protocolId, int packetId) {
      private static final Map<NetworkPacketSummary.PacketIdentification, String> PACKET_NAME_BY_ID;

      public String packetName() {
         return PACKET_NAME_BY_ID.getOrDefault(this, "unknown");
      }

      public static NetworkPacketSummary.PacketIdentification from(RecordedEvent pEvent) {
         return new NetworkPacketSummary.PacketIdentification(pEvent.getEventType().getName().equals("minecraft.PacketSent") ? PacketFlow.CLIENTBOUND : PacketFlow.SERVERBOUND, pEvent.getString("protocolId"), pEvent.getInt("packetId"));
      }

      static {
         ImmutableMap.Builder<NetworkPacketSummary.PacketIdentification, String> builder = ImmutableMap.builder();

         for(ConnectionProtocol connectionprotocol : ConnectionProtocol.values()) {
            for(PacketFlow packetflow : PacketFlow.values()) {
               Int2ObjectMap<Class<? extends Packet<?>>> int2objectmap = connectionprotocol.getPacketsByIds(packetflow);
               int2objectmap.forEach((p_296652_, p_296653_) -> {
                  builder.put(new NetworkPacketSummary.PacketIdentification(packetflow, connectionprotocol.id(), p_296652_), p_296653_.getSimpleName());
               });
            }
         }

         PACKET_NAME_BY_ID = builder.build();
      }
   }
}