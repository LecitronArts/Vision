package net.minecraft.advancements.critereon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public record LightningBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, Optional<EntityPredicate> entityStruck) implements EntitySubPredicate {
   public static final MapCodec<LightningBoltPredicate> CODEC = RecordCodecBuilder.mapCodec((p_298937_) -> {
      return p_298937_.group(ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "blocks_set_on_fire", MinMaxBounds.Ints.ANY).forGetter(LightningBoltPredicate::blocksSetOnFire), ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "entity_struck").forGetter(LightningBoltPredicate::entityStruck)).apply(p_298937_, LightningBoltPredicate::new);
   });

   public static LightningBoltPredicate blockSetOnFire(MinMaxBounds.Ints pBlocksSetOnFire) {
      return new LightningBoltPredicate(pBlocksSetOnFire, Optional.empty());
   }

   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.LIGHTNING;
   }

   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      if (!(pEntity instanceof LightningBolt lightningbolt)) {
         return false;
      } else {
         return this.blocksSetOnFire.matches(lightningbolt.getBlocksSetOnFire()) && (this.entityStruck.isEmpty() || lightningbolt.getHitEntities().anyMatch((p_299409_) -> {
            return this.entityStruck.get().matches(pLevel, pPosition, p_299409_);
         }));
      }
   }
}