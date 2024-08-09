package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   public Codec<RecipeCraftedTrigger.TriggerInstance> codec() {
      return RecipeCraftedTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, ResourceLocation pRecipeId, List<ItemStack> pItems) {
      this.trigger(pPlayer, (p_282798_) -> {
         return p_282798_.matches(pRecipeId, pItems);
      });
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceLocation recipeId, List<ItemPredicate> ingredients) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<RecipeCraftedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create((p_313012_) -> {
         return p_313012_.group(ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(RecipeCraftedTrigger.TriggerInstance::player), ResourceLocation.CODEC.fieldOf("recipe_id").forGetter(RecipeCraftedTrigger.TriggerInstance::recipeId), ExtraCodecs.strictOptionalField(ItemPredicate.CODEC.listOf(), "ingredients", List.of()).forGetter(RecipeCraftedTrigger.TriggerInstance::ingredients)).apply(p_313012_, RecipeCraftedTrigger.TriggerInstance::new);
      });

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation pRecipeId, List<ItemPredicate.Builder> pIngredients) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), pRecipeId, pIngredients.stream().map(ItemPredicate.Builder::build).toList()));
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation pRecipeId) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), pRecipeId, List.of()));
      }

      boolean matches(ResourceLocation pRecipeId, List<ItemStack> pItems) {
         if (!pRecipeId.equals(this.recipeId)) {
            return false;
         } else {
            List<ItemStack> list = new ArrayList<>(pItems);

            for(ItemPredicate itempredicate : this.ingredients) {
               boolean flag = false;
               Iterator<ItemStack> iterator = list.iterator();

               while(iterator.hasNext()) {
                  if (itempredicate.matches(iterator.next())) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }

            return true;
         }
      }

      public Optional<ContextAwarePredicate> player() {
         return this.player;
      }
   }
}