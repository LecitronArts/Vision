package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SequentialEntry extends CompositeEntryBase {
   public static final Codec<SequentialEntry> CODEC = createCodec(SequentialEntry::new);

   SequentialEntry(List<LootPoolEntryContainer> p_297558_, List<LootItemCondition> p_299835_) {
      super(p_297558_, p_299835_);
   }

   public LootPoolEntryType getType() {
      return LootPoolEntries.SEQUENCE;
   }

   protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
      ComposableEntryContainer composableentrycontainer;
      switch (pChildren.size()) {
         case 0:
            composableentrycontainer = ALWAYS_TRUE;
            break;
         case 1:
            composableentrycontainer = pChildren.get(0);
            break;
         case 2:
            composableentrycontainer = pChildren.get(0).and(pChildren.get(1));
            break;
         default:
            composableentrycontainer = (p_297043_, p_297044_) -> {
               for(ComposableEntryContainer composableentrycontainer1 : pChildren) {
                  if (!composableentrycontainer1.expand(p_297043_, p_297044_)) {
                     return false;
                  }
               }

               return true;
            };
      }

      return composableentrycontainer;
   }

   public static SequentialEntry.Builder sequential(LootPoolEntryContainer.Builder<?>... pChildren) {
      return new SequentialEntry.Builder(pChildren);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<SequentialEntry.Builder> {
      private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
         for(LootPoolEntryContainer.Builder<?> builder : pChildren) {
            this.entries.add(builder.build());
         }

      }

      protected SequentialEntry.Builder getThis() {
         return this;
      }

      public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         this.entries.add(pChildBuilder.build());
         return this;
      }

      public LootPoolEntryContainer build() {
         return new SequentialEntry(this.entries.build(), this.getConditions());
      }
   }
}