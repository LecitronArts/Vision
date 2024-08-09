package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeEntryBase extends LootPoolEntryContainer {
   protected final List<LootPoolEntryContainer> children;
   private final ComposableEntryContainer composedChildren;

   protected CompositeEntryBase(List<LootPoolEntryContainer> pChildren, List<LootItemCondition> pConditions) {
      super(pConditions);
      this.children = pChildren;
      this.composedChildren = this.compose(pChildren);
   }

   public void validate(ValidationContext pValidationContext) {
      super.validate(pValidationContext);
      if (this.children.isEmpty()) {
         pValidationContext.reportProblem("Empty children list");
      }

      for(int i = 0; i < this.children.size(); ++i) {
         this.children.get(i).validate(pValidationContext.forChild(".entry[" + i + "]"));
      }

   }

   protected abstract ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren);

   public final boolean expand(LootContext pLootContext, Consumer<LootPoolEntry> pEntryConsumer) {
      return !this.canRun(pLootContext) ? false : this.composedChildren.expand(pLootContext, pEntryConsumer);
   }

   public static <T extends CompositeEntryBase> Codec<T> createCodec(CompositeEntryBase.CompositeEntryConstructor<T> pConstructor) {
      return RecordCodecBuilder.create((p_299098_) -> {
         return p_299098_.group(ExtraCodecs.strictOptionalField(LootPoolEntries.CODEC.listOf(), "children", List.of()).forGetter((p_300130_) -> {
            return p_300130_.children;
         })).and(commonFields(p_299098_).t1()).apply(p_299098_, pConstructor::create);
      });
   }

   @FunctionalInterface
   public interface CompositeEntryConstructor<T extends CompositeEntryBase> {
      T create(List<LootPoolEntryContainer> pChildren, List<LootItemCondition> pConditions);
   }
}