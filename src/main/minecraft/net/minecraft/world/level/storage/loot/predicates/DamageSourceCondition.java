package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
   public static final Codec<DamageSourceCondition> CODEC = RecordCodecBuilder.create((p_297187_) -> {
      return p_297187_.group(ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "predicate").forGetter(DamageSourceCondition::predicate)).apply(p_297187_, DamageSourceCondition::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
   }

   public boolean test(LootContext pContext) {
      DamageSource damagesource = pContext.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
      Vec3 vec3 = pContext.getParamOrNull(LootContextParams.ORIGIN);
      if (vec3 != null && damagesource != null) {
         return this.predicate.isEmpty() || this.predicate.get().matches(pContext.getLevel(), vec3, damagesource);
      } else {
         return false;
      }
   }

   public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder pBuilder) {
      return () -> {
         return new DamageSourceCondition(Optional.of(pBuilder.build()));
      };
   }
}