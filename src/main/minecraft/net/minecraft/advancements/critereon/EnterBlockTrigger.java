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

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   public Codec<EnterBlockTrigger.TriggerInstance> codec() {
      return EnterBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, BlockState pState) {
      this.trigger(pPlayer, (p_31277_) -> {
         return p_31277_.matches(pState);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EnterBlockTrigger.TriggerInstance> CODEC = ExtraCodecs.validate(RecordCodecBuilder.create((p_308128_) -> {
         return p_308128_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(EnterBlockTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(BuiltInRegistries.BLOCK.holderByNameCodec(), "block").forGetter(EnterBlockTrigger.TriggerInstance::block), ExtraCodecs.strictOptionalField(StatePropertiesPredicate.CODEC, "state").forGetter(EnterBlockTrigger.TriggerInstance::state)).apply(p_308128_, EnterBlockTrigger.TriggerInstance::new);
      }), EnterBlockTrigger.TriggerInstance::validate);

      private static DataResult<EnterBlockTrigger.TriggerInstance> validate(EnterBlockTrigger.TriggerInstance p_312153_) {
         return p_312153_.block.flatMap((p_308123_) -> {
            return p_312153_.state.flatMap((p_308130_) -> {
               return p_308130_.checkState(((Block)p_308123_.value()).getStateDefinition());
            }).map((p_308125_) -> {
               return DataResult.<EnterBlockTrigger.TriggerInstance>error(() -> {
                  return "Block" + p_308123_ + " has no property " + p_308125_;
               });
            });
         }).orElseGet(() -> {
            return DataResult.success(p_312153_);
         });
      }

      public static Criterion<EnterBlockTrigger.TriggerInstance> entersBlock(Block pBlock) {
         return CriteriaTriggers.ENTER_BLOCK.createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(pBlock.builtInRegistryHolder()), Optional.empty()));
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