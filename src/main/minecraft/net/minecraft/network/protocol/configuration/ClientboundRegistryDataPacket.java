package net.minecraft.network.protocol.configuration;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.RegistryOps;

public record ClientboundRegistryDataPacket(RegistryAccess.Frozen registryHolder) implements Packet<ClientConfigurationPacketListener> {
   private static final RegistryOps<Tag> BUILTIN_CONTEXT_OPS = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

   public ClientboundRegistryDataPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readWithCodecTrusted(BUILTIN_CONTEXT_OPS, RegistrySynchronization.NETWORK_CODEC).freeze());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeWithCodec(BUILTIN_CONTEXT_OPS, RegistrySynchronization.NETWORK_CODEC, this.registryHolder);
   }

   public void handle(ClientConfigurationPacketListener pHandler) {
      pHandler.handleRegistryData(this);
   }
}