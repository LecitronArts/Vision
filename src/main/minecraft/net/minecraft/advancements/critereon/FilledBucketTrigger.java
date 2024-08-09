package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class FilledBucketTrigger extends SimpleCriterionTrigger<FilledBucketTrigger.TriggerInstance> {
   public Codec<FilledBucketTrigger.TriggerInstance> codec() {
      return FilledBucketTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pStack) {
      this.trigger(pPlayer, (p_38777_) -> {
         return p_38777_.matches(pStack);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<FilledBucketTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_308133_) -> {
         return p_308133_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(FilledBucketTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(FilledBucketTrigger.TriggerInstance::item)).apply(p_308133_, FilledBucketTrigger.TriggerInstance::new);
      });

      public static Criterion<FilledBucketTrigger.TriggerInstance> filledBucket(ItemPredicate.Builder pItem) {
         return CriteriaTriggers.FILLED_BUCKET.createCriterion(new FilledBucketTrigger.TriggerInstance(Optional.empty(), Optional.of(pItem.build())));
      }

      public boolean matches(ItemStack pStack) {
         return !this.item.isPresent() || this.item.get().matches(pStack);
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}