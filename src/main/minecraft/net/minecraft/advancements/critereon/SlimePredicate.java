package net.minecraft.advancements.critereon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

public record SlimePredicate(MinMaxBounds.Ints size) implements EntitySubPredicate {
   public static final MapCodec<SlimePredicate> CODEC = RecordCodecBuilder.mapCodec((p_300273_) -> {
      return p_300273_.group(ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "size", MinMaxBounds.Ints.ANY).forGetter(SlimePredicate::size)).apply(p_300273_, SlimePredicate::new);
   });

   public static SlimePredicate sized(MinMaxBounds.Ints pSize) {
      return new SlimePredicate(pSize);
   }

   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      if (pEntity instanceof Slime slime) {
         return this.size.matches(slime.getSize());
      } else {
         return false;
      }
   }

   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.SLIME;
   }
}