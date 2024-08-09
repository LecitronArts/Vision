package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<FunctionReference> CODEC = RecordCodecBuilder.create((p_297101_) -> {
      return commonFields(p_297101_).and(ResourceLocation.CODEC.fieldOf("name").forGetter((p_297100_) -> {
         return p_297100_.name;
      })).apply(p_297101_, FunctionReference::new);
   });
   private final ResourceLocation name;

   private FunctionReference(List<LootItemCondition> p_298565_, ResourceLocation p_279246_) {
      super(p_298565_);
      this.name = p_279246_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.REFERENCE;
   }

   public void validate(ValidationContext pContext) {
      LootDataId<LootItemFunction> lootdataid = new LootDataId<>(LootDataType.MODIFIER, this.name);
      if (pContext.hasVisitedElement(lootdataid)) {
         pContext.reportProblem("Function " + this.name + " is recursively called");
      } else {
         super.validate(pContext);
         pContext.resolver().getElementOptional(lootdataid).ifPresentOrElse((p_279367_) -> {
            p_279367_.validate(pContext.enterElement(".{" + this.name + "}", lootdataid));
         }, () -> {
            pContext.reportProblem("Unknown function table called " + this.name);
         });
      }
   }

   protected ItemStack run(ItemStack pStack, LootContext pContext) {
      LootItemFunction lootitemfunction = pContext.getResolver().getElement(LootDataType.MODIFIER, this.name);
      if (lootitemfunction == null) {
         LOGGER.warn("Unknown function: {}", (Object)this.name);
         return pStack;
      } else {
         LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemfunction);
         if (pContext.pushVisitedElement(visitedentry)) {
            ItemStack itemstack;
            try {
               itemstack = lootitemfunction.apply(pStack, pContext);
            } finally {
               pContext.popVisitedElement(visitedentry);
            }

            return itemstack;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return pStack;
         }
      }
   }

   public static LootItemConditionalFunction.Builder<?> functionReference(ResourceLocation pName) {
      return simpleBuilder((p_297103_) -> {
         return new FunctionReference(p_297103_, pName);
      });
   }
}