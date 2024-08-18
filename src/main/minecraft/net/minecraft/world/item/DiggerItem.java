package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

public class DiggerItem extends TieredItem implements Vanishable {
   private final TagKey<Block> blocks;
   protected final float speed;
   private final float attackDamageBaseline;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;
   private float viaFabricPlus$attackDamage_r1_8;
   private Multimap<Attribute, AttributeModifier> viaFabricPlus$AttributeModifiers_r1_8;

   protected DiggerItem(float pAttackDamageModifier, float pAttackSpeedModifier, Tier pTier, TagKey<Block> pBlocks, Item.Properties pProperties) {
      super(pTier, pProperties);
      this.blocks = pBlocks;
      this.speed = pTier.getSpeed();
      this.attackDamageBaseline = pAttackDamageModifier + pTier.getAttackDamageBonus();
      ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", (double)this.attackDamageBaseline, AttributeModifier.Operation.ADDITION));
      builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double)pAttackSpeedModifier, AttributeModifier.Operation.ADDITION));
      this.defaultModifiers = builder.build();
      final float materialAttackDamage = pTier.getAttackDamageBonus();
      if ((Item) this instanceof PickaxeItem) {
         this.viaFabricPlus$attackDamage_r1_8 = 2 + materialAttackDamage;
      } else if ((Item) this instanceof ShovelItem) {
         this.viaFabricPlus$attackDamage_r1_8 = 1 + materialAttackDamage;
      } else if ((Item) this instanceof AxeItem) {
         this.viaFabricPlus$attackDamage_r1_8 = 3 + materialAttackDamage;
      } else { // HoeItem didn't use MiningToolItem abstraction in 1.8
         this.viaFabricPlus$AttributeModifiers_r1_8 = ImmutableMultimap.of();
         return;
      }

      final ImmutableMultimap.Builder<Attribute, AttributeModifier> builder2 = ImmutableMultimap.builder();
      builder2.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", this.viaFabricPlus$attackDamage_r1_8, AttributeModifier.Operation.ADDITION));
      this.viaFabricPlus$AttributeModifiers_r1_8 = builder2.build();
   }

   public float getDestroySpeed(ItemStack pStack, BlockState pState) {
      return pState.is(this.blocks) ? this.speed : 1.0F;
   }

   public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      pStack.hurtAndBreak(2, pAttacker, (p_41007_) -> {
         p_41007_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
      });
      return true;
   }

   public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
      if (!pLevel.isClientSide && pState.getDestroySpeed(pLevel, pPos) != 0.0F) {
         pStack.hurtAndBreak(1, pEntityLiving, (p_40992_) -> {
            p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
         });
      }

      return true;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pEquipmentSlot) {
      return pEquipmentSlot == EquipmentSlot.MAINHAND ? (DebugSettings.global().replaceAttributeModifiers.isEnabled() ? this.viaFabricPlus$AttributeModifiers_r1_8 : this.defaultModifiers ): super.getDefaultAttributeModifiers(pEquipmentSlot);
   }

   public float getAttackDamage() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return this.viaFabricPlus$attackDamage_r1_8;
      } else {
         return this.attackDamageBaseline;
      }
/*      return this.attackDamageBaseline;*/
   }

   public boolean isCorrectToolForDrops(BlockState pBlock) {
      int i = this.getTier().getLevel();
      if (i < 3 && pBlock.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
         return false;
      } else if (i < 2 && pBlock.is(BlockTags.NEEDS_IRON_TOOL)) {
         return false;
      } else {
         return i < 1 && pBlock.is(BlockTags.NEEDS_STONE_TOOL) ? false : pBlock.is(this.blocks);
      }
   }
}