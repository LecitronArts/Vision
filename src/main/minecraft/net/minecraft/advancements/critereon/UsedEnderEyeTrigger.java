package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
   public Codec<UsedEnderEyeTrigger.TriggerInstance> codec() {
      return UsedEnderEyeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos) {
      double d0 = pPlayer.getX() - (double)pPos.getX();
      double d1 = pPlayer.getZ() - (double)pPos.getZ();
      double d2 = d0 * d0 + d1 * d1;
      this.trigger(pPlayer, (p_73934_) -> {
         return p_73934_.matches(d2);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<UsedEnderEyeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_312855_) -> {
         return p_312855_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(UsedEnderEyeTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "distance", MinMaxBounds.Doubles.ANY).forGetter(UsedEnderEyeTrigger.TriggerInstance::distance)).apply(p_312855_, UsedEnderEyeTrigger.TriggerInstance::new);
      });

      public boolean matches(double pDistanceSq) {
         return this.distance.matchesSqr(pDistanceSq);
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}