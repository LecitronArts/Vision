package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetStewEffectFunction extends LootItemConditionalFunction {
   private static final Codec<List<SetStewEffectFunction.EffectEntry>> EFFECTS_LIST = ExtraCodecs.validate(SetStewEffectFunction.EffectEntry.CODEC.listOf(), (p_297178_) -> {
      Set<Holder<MobEffect>> set = new ObjectOpenHashSet<>();

      for(SetStewEffectFunction.EffectEntry setsteweffectfunction$effectentry : p_297178_) {
         if (!set.add(setsteweffectfunction$effectentry.effect())) {
            return DataResult.error(() -> {
               return "Encountered duplicate mob effect: '" + setsteweffectfunction$effectentry.effect() + "'";
            });
         }
      }

      return DataResult.success(p_297178_);
   });
   public static final Codec<SetStewEffectFunction> CODEC = RecordCodecBuilder.create((p_297177_) -> {
      return commonFields(p_297177_).and(ExtraCodecs.strictOptionalField(EFFECTS_LIST, "effects", List.of()).forGetter((p_297176_) -> {
         return p_297176_.effects;
      })).apply(p_297177_, SetStewEffectFunction::new);
   });
   private final List<SetStewEffectFunction.EffectEntry> effects;

   SetStewEffectFunction(List<LootItemCondition> p_298902_, List<SetStewEffectFunction.EffectEntry> p_298444_) {
      super(p_298902_);
      this.effects = p_298444_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_STEW_EFFECT;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.effects.stream().flatMap((p_297174_) -> {
         return p_297174_.duration().getReferencedContextParams().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (pStack.is(Items.SUSPICIOUS_STEW) && !this.effects.isEmpty()) {
         SetStewEffectFunction.EffectEntry setsteweffectfunction$effectentry = Util.getRandom(this.effects, pContext.getRandom());
         MobEffect mobeffect = setsteweffectfunction$effectentry.effect().value();
         int i = setsteweffectfunction$effectentry.duration().getInt(pContext);
         if (!mobeffect.isInstantenous()) {
            i *= 20;
         }

         SuspiciousStewItem.appendMobEffects(pStack, List.of(new SuspiciousEffectHolder.EffectEntry(mobeffect, i)));
         return pStack;
      } else {
         return pStack;
      }
   }

   public static SetStewEffectFunction.Builder stewEffect() {
      return new SetStewEffectFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetStewEffectFunction.Builder> {
      private final ImmutableList.Builder<SetStewEffectFunction.EffectEntry> effects = ImmutableList.builder();

      protected SetStewEffectFunction.Builder getThis() {
         return this;
      }

      public SetStewEffectFunction.Builder withEffect(MobEffect pEffect, NumberProvider pDurationValue) {
         this.effects.add(new SetStewEffectFunction.EffectEntry(pEffect.builtInRegistryHolder(), pDurationValue));
         return this;
      }

      public LootItemFunction build() {
         return new SetStewEffectFunction(this.getConditions(), this.effects.build());
      }
   }

   static record EffectEntry(Holder<MobEffect> effect, NumberProvider duration) {
      public static final Codec<SetStewEffectFunction.EffectEntry> CODEC = RecordCodecBuilder.create((p_298252_) -> {
         return p_298252_.group(BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("type").forGetter(SetStewEffectFunction.EffectEntry::effect), NumberProviders.CODEC.fieldOf("duration").forGetter(SetStewEffectFunction.EffectEntry::duration)).apply(p_298252_, SetStewEffectFunction.EffectEntry::new);
      });
   }
}