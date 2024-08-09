package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootTableReference extends LootPoolSingletonContainer {
   public static final Codec<LootTableReference> CODEC = RecordCodecBuilder.create((p_297041_) -> {
      return p_297041_.group(ResourceLocation.CODEC.fieldOf("name").forGetter((p_297035_) -> {
         return p_297035_.name;
      })).and(singletonFields(p_297041_)).apply(p_297041_, LootTableReference::new);
   });
   private final ResourceLocation name;

   private LootTableReference(ResourceLocation p_79756_, int p_79757_, int p_79758_, List<LootItemCondition> p_297470_, List<LootItemFunction> p_299264_) {
      super(p_79757_, p_79758_, p_297470_, p_299264_);
      this.name = p_79756_;
   }

   public LootPoolEntryType getType() {
      return LootPoolEntries.REFERENCE;
   }

   public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
      LootTable loottable = pLootContext.getResolver().getLootTable(this.name);
      loottable.getRandomItemsRaw(pLootContext, pStackConsumer);
   }

   public void validate(ValidationContext pValidationContext) {
      LootDataId<LootTable> lootdataid = new LootDataId<>(LootDataType.TABLE, this.name);
      if (pValidationContext.hasVisitedElement(lootdataid)) {
         pValidationContext.reportProblem("Table " + this.name + " is recursively called");
      } else {
         super.validate(pValidationContext);
         pValidationContext.resolver().getElementOptional(lootdataid).ifPresentOrElse((p_279078_) -> {
            p_279078_.validate(pValidationContext.enterElement("->{" + this.name + "}", lootdataid));
         }, () -> {
            pValidationContext.reportProblem("Unknown loot table called " + this.name);
         });
      }
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceLocation pTable) {
      return simpleBuilder((p_297037_, p_297038_, p_297039_, p_297040_) -> {
         return new LootTableReference(pTable, p_297037_, p_297038_, p_297039_, p_297040_);
      });
   }
}