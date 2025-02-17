package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.function.Supplier;

import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public enum ArmorMaterials implements StringRepresentable, ArmorMaterial {
   LEATHER("leather", 5, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266652_) -> {
      p_266652_.put(ArmorItem.Type.BOOTS, 1);
      p_266652_.put(ArmorItem.Type.LEGGINGS, 2);
      p_266652_.put(ArmorItem.Type.CHESTPLATE, 3);
      p_266652_.put(ArmorItem.Type.HELMET, 1);
   }), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, () -> {
      return Ingredient.of(Items.LEATHER);
   }),
   CHAIN("chainmail", 15, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266651_) -> {
      p_266651_.put(ArmorItem.Type.BOOTS, 1);
      p_266651_.put(ArmorItem.Type.LEGGINGS, 4);
      p_266651_.put(ArmorItem.Type.CHESTPLATE, 5);
      p_266651_.put(ArmorItem.Type.HELMET, 2);
   }), 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, () -> {
      return Ingredient.of(Items.IRON_INGOT);
   }),
   IRON("iron", 15, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266654_) -> {
      p_266654_.put(ArmorItem.Type.BOOTS, 2);
      p_266654_.put(ArmorItem.Type.LEGGINGS, 5);
      p_266654_.put(ArmorItem.Type.CHESTPLATE, 6);
      p_266654_.put(ArmorItem.Type.HELMET, 2);
   }), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, () -> {
      return Ingredient.of(Items.IRON_INGOT);
   }),
   GOLD("gold", 7, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266650_) -> {
      p_266650_.put(ArmorItem.Type.BOOTS, 1);
      p_266650_.put(ArmorItem.Type.LEGGINGS, 3);
      p_266650_.put(ArmorItem.Type.CHESTPLATE, 5);
      p_266650_.put(ArmorItem.Type.HELMET, 2);
   }), 25, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F, () -> {
      return Ingredient.of(Items.GOLD_INGOT);
   }),
   DIAMOND("diamond", 33, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266649_) -> {
      p_266649_.put(ArmorItem.Type.BOOTS, 3);
      p_266649_.put(ArmorItem.Type.LEGGINGS, 6);
      p_266649_.put(ArmorItem.Type.CHESTPLATE, 8);
      p_266649_.put(ArmorItem.Type.HELMET, 3);
   }), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F, () -> {
      return Ingredient.of(Items.DIAMOND);
   }),
   TURTLE("turtle", 25, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266656_) -> {
      p_266656_.put(ArmorItem.Type.BOOTS, 2);
      p_266656_.put(ArmorItem.Type.LEGGINGS, 5);
      p_266656_.put(ArmorItem.Type.CHESTPLATE, 6);
      p_266656_.put(ArmorItem.Type.HELMET, 2);
   }), 9, SoundEvents.ARMOR_EQUIP_TURTLE, 0.0F, 0.0F, () -> {
      return Ingredient.of(Items.SCUTE);
   }),
   NETHERITE("netherite", 37, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266655_) -> {
      p_266655_.put(ArmorItem.Type.BOOTS, 3);
      p_266655_.put(ArmorItem.Type.LEGGINGS, 6);
      p_266655_.put(ArmorItem.Type.CHESTPLATE, 8);
      p_266655_.put(ArmorItem.Type.HELMET, 3);
   }), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, () -> {
      return Ingredient.of(Items.NETHERITE_INGOT);
   });

   public static final Codec<ArmorMaterials> CODEC = StringRepresentable.fromEnum(ArmorMaterials::values);
   private static final EnumMap<ArmorItem.Type, Integer> HEALTH_FUNCTION_FOR_TYPE = Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266653_) -> {
      p_266653_.put(ArmorItem.Type.BOOTS, 13);
      p_266653_.put(ArmorItem.Type.LEGGINGS, 15);
      p_266653_.put(ArmorItem.Type.CHESTPLATE, 16);
      p_266653_.put(ArmorItem.Type.HELMET, 11);
   });
   private final String name;
   private final int durabilityMultiplier;
   private final EnumMap<ArmorItem.Type, Integer> protectionFunctionForType;
   private final int enchantmentValue;
   private final SoundEvent sound;
   private final float toughness;
   private final float knockbackResistance;
   private final LazyLoadedValue<Ingredient> repairIngredient;

   private ArmorMaterials(String pName, int pDurabilityMultiplier, EnumMap<ArmorItem.Type, Integer> pProtectionFunctionForType, int pEnchantmentValue, SoundEvent pSound, float pToughness, float pKnockbackResistance, Supplier<Ingredient> pRepairIngredient) {
      this.name = pName;
      this.durabilityMultiplier = pDurabilityMultiplier;
      this.protectionFunctionForType = pProtectionFunctionForType;
      this.enchantmentValue = pEnchantmentValue;
      this.sound = pSound;
      this.toughness = pToughness;
      this.knockbackResistance = pKnockbackResistance;
      this.repairIngredient = new LazyLoadedValue<>(pRepairIngredient);
   }

   public int getDurabilityForType(ArmorItem.Type pType) {
      int viaFixDurability;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         if (this == ArmorMaterials.LEATHER) {
            viaFixDurability = 3;
         } else if (this == ArmorMaterials.CHAIN || this == ArmorMaterials.GOLD) {
            viaFixDurability = 6;
         } else if (this == ArmorMaterials.IRON) {
            viaFixDurability = 12;
         } else if (this == ArmorMaterials.DIAMOND) {
            viaFixDurability = 24;
         } else {
            viaFixDurability = this.durabilityMultiplier;
         }
      } else {
         viaFixDurability = this.durabilityMultiplier;
      }

      return HEALTH_FUNCTION_FOR_TYPE.get(pType) * viaFixDurability;
   }

   public int getDefenseForType(ArmorItem.Type pType) {
      return this.protectionFunctionForType.get(pType);
   }

   public int getEnchantmentValue() {
      return this.enchantmentValue;
   }

   public SoundEvent getEquipSound() {
      return this.sound;
   }

   public Ingredient getRepairIngredient() {
      return this.repairIngredient.get();
   }

   public String getName() {
      return this.name;
   }

   public float getToughness() {
      return this.toughness;
   }

   public float getKnockbackResistance() {
      return this.knockbackResistance;
   }

   public String getSerializedName() {
      return this.name;
   }
}