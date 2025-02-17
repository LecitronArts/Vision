package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;

public interface ClientConfigurationPacketListener extends ClientCommonPacketListener {
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.CONFIGURATION;
   }

   void handleConfigurationFinished(ClientboundFinishConfigurationPacket pPacket);

   void handleRegistryData(ClientboundRegistryDataPacket pPacket);

   void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket pPacket);
}