package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
   public Codec<EnchantedItemTrigger.TriggerInstance> codec() {
      return EnchantedItemTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem, int pLevelsSpent) {
      this.trigger(pPlayer, (p_27675_) -> {
         return p_27675_.matches(pItem, pLevelsSpent);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints levels) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EnchantedItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_308121_) -> {
         return p_308121_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(EnchantedItemTrigger.TriggerInstance::player), ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(EnchantedItemTrigger.TriggerInstance::item), ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "levels", MinMaxBounds.Ints.ANY).forGetter(EnchantedItemTrigger.TriggerInstance::levels)).apply(p_308121_, EnchantedItemTrigger.TriggerInstance::new);
      });

      public static Criterion<EnchantedItemTrigger.TriggerInstance> enchantedItem() {
         return CriteriaTriggers.ENCHANTED_ITEM.createCriterion(new EnchantedItemTrigger.TriggerInstance(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack pItem, int pLevels) {
         if (this.item.isPresent() && !this.item.get().matches(pItem)) {
            return false;
         } else {
            return this.levels.matches(pLevels);
         }
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}