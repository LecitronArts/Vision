package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

public interface PositionSourceType<T extends PositionSource> {
   PositionSourceType<BlockPositionSource> BLOCK = register("block", new BlockPositionSource.Type());
   PositionSourceType<EntityPositionSource> ENTITY = register("entity", new EntityPositionSource.Type());

   T read(FriendlyByteBuf pByteBuf);

   void write(FriendlyByteBuf pByteBuf, T pSource);

   Codec<T> codec();

   static <S extends PositionSourceType<T>, T extends PositionSource> S register(String pId, S pType) {
      return Registry.register(BuiltInRegistries.POSITION_SOURCE_TYPE, pId, pType);
   }

   static PositionSource fromNetwork(FriendlyByteBuf pByteBuf) {
      PositionSourceType<?> positionsourcetype = pByteBuf.readById(BuiltInRegistries.POSITION_SOURCE_TYPE);
      if (positionsourcetype == null) {
         throw new IllegalArgumentException("Unknown position source type");
      } else {
         return positionsourcetype.read(pByteBuf);
      }
   }

   static <T extends PositionSource> void toNetwork(T pSource, FriendlyByteBuf pByteBuf) {
      pByteBuf.writeId(BuiltInRegistries.POSITION_SOURCE_TYPE, pSource.getType());
      ((PositionSourceType)pSource.getType()).write(pByteBuf, pSource);
   }
}