package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record CommonPlayerSpawnInfo(ResourceKey<DimensionType> dimensionType, ResourceKey<Level> dimension, long seed, GameType gameType, @Nullable GameType previousGameType, boolean isDebug, boolean isFlat, Optional<GlobalPos> lastDeathLocation, int portalCooldown) {
   public CommonPlayerSpawnInfo(FriendlyByteBuf pBuffer) {
      this(pBuffer.readResourceKey(Registries.DIMENSION_TYPE), pBuffer.readResourceKey(Registries.DIMENSION), pBuffer.readLong(), GameType.byId(pBuffer.readByte()), GameType.byNullableId(pBuffer.readByte()), pBuffer.readBoolean(), pBuffer.readBoolean(), pBuffer.readOptional(FriendlyByteBuf::readGlobalPos), pBuffer.readVarInt());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceKey(this.dimensionType);
      pBuffer.writeResourceKey(this.dimension);
      pBuffer.writeLong(this.seed);
      pBuffer.writeByte(this.gameType.getId());
      pBuffer.writeByte(GameType.getNullableId(this.previousGameType));
      pBuffer.writeBoolean(this.isDebug);
      pBuffer.writeBoolean(this.isFlat);
      pBuffer.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
      pBuffer.writeVarInt(this.portalCooldown);
   }
}