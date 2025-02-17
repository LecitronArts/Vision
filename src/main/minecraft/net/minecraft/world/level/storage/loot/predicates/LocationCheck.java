package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LocationCheck(Optional<LocationPredicate> predicate, BlockPos offset) implements LootItemCondition {
   private static final MapCodec<BlockPos> OFFSET_CODEC = RecordCodecBuilder.mapCodec((p_297192_) -> {
      return p_297192_.group(ExtraCodecs.strictOptionalField(Codec.INT, "offsetX", 0).forGetter(Vec3i::getX), ExtraCodecs.strictOptionalField(Codec.INT, "offsetY", 0).forGetter(Vec3i::getY), ExtraCodecs.strictOptionalField(Codec.INT, "offsetZ", 0).forGetter(Vec3i::getZ)).apply(p_297192_, BlockPos::new);
   });
   public static final Codec<LocationCheck> CODEC = RecordCodecBuilder.create((p_297194_) -> {
      return p_297194_.group(ExtraCodecs.strictOptionalField(LocationPredicate.CODEC, "predicate").forGetter(LocationCheck::predicate), OFFSET_CODEC.forGetter(LocationCheck::offset)).apply(p_297194_, LocationCheck::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.LOCATION_CHECK;
   }

   public boolean test(LootContext pContext) {
      Vec3 vec3 = pContext.getParamOrNull(LootContextParams.ORIGIN);
      return vec3 != null && (this.predicate.isEmpty() || this.predicate.get().matches(pContext.getLevel(), vec3.x() + (double)this.offset.getX(), vec3.y() + (double)this.offset.getY(), vec3.z() + (double)this.offset.getZ()));
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder pLocationPredicateBuilder) {
      return () -> {
         return new LocationCheck(Optional.of(pLocationPredicateBuilder.build()), BlockPos.ZERO);
      };
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder pLocationPredicateBuilder, BlockPos pOffset) {
      return () -> {
         return new LocationCheck(Optional.of(pLocationPredicateBuilder.build()), pOffset);
      };
   }
}