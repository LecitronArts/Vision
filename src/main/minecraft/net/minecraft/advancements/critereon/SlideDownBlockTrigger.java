package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SlideDownBlockTrigger extends SimpleCriterionTrigger<SlideDownBlockTrigger.TriggerInstance> {
   public Codec<SlideDownBlockTrigger.TriggerInstance> codec() {
      return SlideDownBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, BlockState pState) {
      this.trigger(pPlayer, (p_66986_) -> {
         return p_66986_.matches(pState);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SlideDownBlockTrigger.TriggerInstance> CODEC = ExtraCodecs.validate(RecordCodecBuilder.create((p_308152_) -> {
         return p_308152_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(SlideDownBlockTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(BuiltInRegistries.BLOCK.holderByNameCodec(), "block").forGetter(SlideDownBlockTrigger.TriggerInstance::block), ExtraCodecs.strictOptionalField(StatePropertiesPredicate.CODEC, "state").forGetter(SlideDownBlockTrigger.TriggerInstance::state)).apply(p_308152_, SlideDownBlockTrigger.TriggerInstance::new);
      }), SlideDownBlockTrigger.TriggerInstance::validate);

      private static DataResult<SlideDownBlockTrigger.TriggerInstance> validate(SlideDownBlockTrigger.TriggerInstance p_312534_) {
         return p_312534_.block.flatMap((p_308148_) -> {
            return p_312534_.state.flatMap((p_308151_) -> {
               return p_308151_.checkState(((Block)p_308148_.value()).getStateDefinition());
            }).map((p_308154_) -> {
               return DataResult.<SlideDownBlockTrigger.TriggerInstance>error(() -> {
                  return "Block" + p_308148_ + " has no property " + p_308154_;
               });
            });
         }).orElseGet(() -> {
            return DataResult.success(p_312534_);
         });
      }

      public static Criterion<SlideDownBlockTrigger.TriggerInstance> slidesDownBlock(Block pBlock) {
         return CriteriaTriggers.HONEY_BLOCK_SLIDE.createCriterion(new SlideDownBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(pBlock.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(BlockState pState) {
         if (this.block.isPresent() && !pState.is(this.block.get())) {
            return false;
         } else {
            return !this.state.isPresent() || this.state.get().matches(pState);
         }
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}