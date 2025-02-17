package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;

public class PotionBrewing {
   public static final int BREWING_TIME_SECONDS = 20;
   private static final List<PotionBrewing.Mix<Potion>> POTION_MIXES = Lists.newArrayList();
   private static final List<PotionBrewing.Mix<Item>> CONTAINER_MIXES = Lists.newArrayList();
   private static final List<Ingredient> ALLOWED_CONTAINERS = Lists.newArrayList();
   private static final Predicate<ItemStack> ALLOWED_CONTAINER = (p_43528_) -> {
      for(Ingredient ingredient : ALLOWED_CONTAINERS) {
         if (ingredient.test(p_43528_)) {
            return true;
         }
      }

      return false;
   };

   public static boolean isIngredient(ItemStack pInput) {
      return isContainerIngredient(pInput) || isPotionIngredient(pInput);
   }

   protected static boolean isContainerIngredient(ItemStack pInput) {
      for(PotionBrewing.Mix<Item> mix : CONTAINER_MIXES) {
         if (mix.ingredient.test(pInput)) {
            return true;
         }
      }

      return false;
   }

   protected static boolean isPotionIngredient(ItemStack pInput) {
      for(PotionBrewing.Mix<Potion> mix : POTION_MIXES) {
         if (mix.ingredient.test(pInput)) {
            return true;
         }
      }

      return false;
   }

   public static boolean isBrewablePotion(Potion pPotion) {
      for(PotionBrewing.Mix<Potion> mix : POTION_MIXES) {
         if (mix.to == pPotion) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasMix(ItemStack pInput, ItemStack pReagent) {
      if (!ALLOWED_CONTAINER.test(pInput)) {
         return false;
      } else {
         return hasContainerMix(pInput, pReagent) || hasPotionMix(pInput, pReagent);
      }
   }

   protected static boolean hasContainerMix(ItemStack pInput, ItemStack pReagent) {
      Item item = pInput.getItem();

      for(PotionBrewing.Mix<Item> mix : CONTAINER_MIXES) {
         if (mix.from == item && mix.ingredient.test(pReagent)) {
            return true;
         }
      }

      return false;
   }

   protected static boolean hasPotionMix(ItemStack pInput, ItemStack pReagent) {
      Potion potion = PotionUtils.getPotion(pInput);

      for(PotionBrewing.Mix<Potion> mix : POTION_MIXES) {
         if (mix.from == potion && mix.ingredient.test(pReagent)) {
            return true;
         }
      }

      return false;
   }

   public static ItemStack mix(ItemStack pReagent, ItemStack pPotion) {
      if (!pPotion.isEmpty()) {
         Potion potion = PotionUtils.getPotion(pPotion);
         Item item = pPotion.getItem();

         for(PotionBrewing.Mix<Item> mix : CONTAINER_MIXES) {
            if (mix.from == item && mix.ingredient.test(pReagent)) {
               return PotionUtils.setPotion(new ItemStack(mix.to), potion);
            }
         }

         for(PotionBrewing.Mix<Potion> mix1 : POTION_MIXES) {
            if (mix1.from == potion && mix1.ingredient.test(pReagent)) {
               return PotionUtils.setPotion(new ItemStack(item), mix1.to);
            }
         }
      }

      return pPotion;
   }

   public static void bootStrap() {
      addContainer(Items.POTION);
      addContainer(Items.SPLASH_POTION);
      addContainer(Items.LINGERING_POTION);
      addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
      addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
      addMix(Potions.WATER, Items.GLISTERING_MELON_SLICE, Potions.MUNDANE);
      addMix(Potions.WATER, Items.GHAST_TEAR, Potions.MUNDANE);
      addMix(Potions.WATER, Items.RABBIT_FOOT, Potions.MUNDANE);
      addMix(Potions.WATER, Items.BLAZE_POWDER, Potions.MUNDANE);
      addMix(Potions.WATER, Items.SPIDER_EYE, Potions.MUNDANE);
      addMix(Potions.WATER, Items.SUGAR, Potions.MUNDANE);
      addMix(Potions.WATER, Items.MAGMA_CREAM, Potions.MUNDANE);
      addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
      addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
      addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
      addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
      addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
      addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
      addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
      addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
      addMix(Potions.AWKWARD, Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
      addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
      addMix(Potions.AWKWARD, Items.RABBIT_FOOT, Potions.LEAPING);
      addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
      addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
      addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
      addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
      addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
      addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
      addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
      addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      addMix(Potions.AWKWARD, Items.SUGAR, Potions.SWIFTNESS);
      addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
      addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
      addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
      addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
      addMix(Potions.AWKWARD, Items.GLISTERING_MELON_SLICE, Potions.HEALING);
      addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
      addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
      addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      addMix(Potions.AWKWARD, Items.SPIDER_EYE, Potions.POISON);
      addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
      addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
      addMix(Potions.AWKWARD, Items.GHAST_TEAR, Potions.REGENERATION);
      addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
      addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
      addMix(Potions.AWKWARD, Items.BLAZE_POWDER, Potions.STRENGTH);
      addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
      addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
      addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
      addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
      addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
      addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
   }

   private static void addContainerRecipe(Item pFrom, Item pIngredient, Item pTo) {
      if (!(pFrom instanceof PotionItem)) {
         throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(pFrom));
      } else if (!(pTo instanceof PotionItem)) {
         throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(pTo));
      } else {
         CONTAINER_MIXES.add(new PotionBrewing.Mix<>(pFrom, Ingredient.of(pIngredient), pTo));
      }
   }

   private static void addContainer(Item pContainer) {
      if (!(pContainer instanceof PotionItem)) {
         throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(pContainer));
      } else {
         ALLOWED_CONTAINERS.add(Ingredient.of(pContainer));
      }
   }

   private static void addMix(Potion pPotionEntry, Item pPotionIngredient, Potion pPotionResult) {
      POTION_MIXES.add(new PotionBrewing.Mix<>(pPotionEntry, Ingredient.of(pPotionIngredient), pPotionResult));
   }

   static class Mix<T> {
      final T from;
      final Ingredient ingredient;
      final T to;

      public Mix(T pFrom, Ingredient pIngredient, T pTo) {
         this.from = pFrom;
         this.ingredient = pIngredient;
         this.to = pTo;
      }
   }
}