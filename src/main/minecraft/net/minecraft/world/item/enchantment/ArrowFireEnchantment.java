package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class ArrowFireEnchantment extends Enchantment {
   public ArrowFireEnchantment(Enchantment.Rarity pRarity, EquipmentSlot... pApplicableSlots) {
      super(pRarity, EnchantmentCategory.BOW, pApplicableSlots);
   }

   public int getMinCost(int pEnchantmentLevel) {
      return 20;
   }

   public int getMaxCost(int pEnchantmentLevel) {
      return 50;
   }
}