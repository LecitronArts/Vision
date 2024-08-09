package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public record FishingHookPredicate(Optional<Boolean> inOpenWater) implements EntitySubPredicate {
   public static final FishingHookPredicate ANY = new FishingHookPredicate(Optional.empty());
   public static final MapCodec<FishingHookPredicate> CODEC = RecordCodecBuilder.mapCodec((p_297854_) -> {
      return p_297854_.group(ExtraCodecs.strictOptionalField(Codec.BOOL, "in_open_water").forGetter(FishingHookPredicate::inOpenWater)).apply(p_297854_, FishingHookPredicate::new);
   });

   public static FishingHookPredicate inOpenWater(boolean pInOpenWater) {
      return new FishingHookPredicate(Optional.of(pInOpenWater));
   }

   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.FISHING_HOOK;
   }

   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      if (this.inOpenWater.isEmpty()) {
         return true;
      } else if (pEntity instanceof FishingHook) {
         FishingHook fishinghook = (FishingHook)pEntity;
         return this.inOpenWater.get() == fishinghook.isOpenWaterFishing();
      } else {
         return false;
      }
   }
}