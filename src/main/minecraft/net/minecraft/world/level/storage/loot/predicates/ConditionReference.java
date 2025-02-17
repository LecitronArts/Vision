package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record ConditionReference(ResourceLocation name) implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<ConditionReference> CODEC = RecordCodecBuilder.create((p_297185_) -> {
      return p_297185_.group(ResourceLocation.CODEC.fieldOf("name").forGetter(ConditionReference::name)).apply(p_297185_, ConditionReference::new);
   });

   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   public void validate(ValidationContext pContext) {
      LootDataId<LootItemCondition> lootdataid = new LootDataId<>(LootDataType.PREDICATE, this.name);
      if (pContext.hasVisitedElement(lootdataid)) {
         pContext.reportProblem("Condition " + this.name + " is recursively called");
      } else {
         LootItemCondition.super.validate(pContext);
         pContext.resolver().getElementOptional(lootdataid).ifPresentOrElse((p_279085_) -> {
            p_279085_.validate(pContext.enterElement(".{" + this.name + "}", lootdataid));
         }, () -> {
            pContext.reportProblem("Unknown condition table called " + this.name);
         });
      }
   }

   public boolean test(LootContext pContext) {
      LootItemCondition lootitemcondition = pContext.getResolver().getElement(LootDataType.PREDICATE, this.name);
      if (lootitemcondition == null) {
         LOGGER.warn("Tried using unknown condition table called {}", (Object)this.name);
         return false;
      } else {
         LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemcondition);
         if (pContext.pushVisitedElement(visitedentry)) {
            boolean flag;
            try {
               flag = lootitemcondition.test(pContext);
            } finally {
               pContext.popVisitedElement(visitedentry);
            }

            return flag;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(ResourceLocation pReferencedCondition) {
      return () -> {
         return new ConditionReference(pReferencedCondition);
      };
   }
}