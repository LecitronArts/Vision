package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record InvertedLootItemCondition(LootItemCondition term) implements LootItemCondition {
   public static final Codec<InvertedLootItemCondition> CODEC = RecordCodecBuilder.create((p_297189_) -> {
      return p_297189_.group(LootItemConditions.CODEC.fieldOf("term").forGetter(InvertedLootItemCondition::term)).apply(p_297189_, InvertedLootItemCondition::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.INVERTED;
   }

   public boolean test(LootContext pContext) {
      return !this.term.test(pContext);
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.term.getReferencedContextParams();
   }

   public void validate(ValidationContext pContext) {
      LootItemCondition.super.validate(pContext);
      this.term.validate(pContext);
   }

   public static LootItemCondition.Builder invert(LootItemCondition.Builder pToInvert) {
      InvertedLootItemCondition invertedlootitemcondition = new InvertedLootItemCondition(pToInvert.build());
      return () -> {
         return invertedlootitemcondition;
      };
   }
}