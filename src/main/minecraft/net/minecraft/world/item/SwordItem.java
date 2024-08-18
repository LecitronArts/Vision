package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Unique;

public class SwordItem extends TieredItem implements Vanishable {
   private final float attackDamage;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;
   private float viaFabricPlus$attackDamage_r1_8;
   private Multimap<Attribute, AttributeModifier> viaFabricPlus$AttributeModifiers_r1_8;
   public SwordItem(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Item.Properties pProperties) {
      super(pTier, pProperties);
      this.attackDamage = (float)pAttackDamageModifier + pTier.getAttackDamageBonus();
      ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double)this.attackDamage, AttributeModifier.Operation.ADDITION));
      builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double)pAttackSpeedModifier, AttributeModifier.Operation.ADDITION));
      this.defaultModifiers = builder.build();
      this.viaFabricPlus$attackDamage_r1_8 = 4 + pTier.getAttackDamageBonus();
      final ImmutableMultimap.Builder<Attribute, AttributeModifier> modifierBuilder = ImmutableMultimap.builder();
      modifierBuilder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", this.viaFabricPlus$attackDamage_r1_8, AttributeModifier.Operation.ADDITION));
      this.viaFabricPlus$AttributeModifiers_r1_8 = modifierBuilder.build();
   }
   @Override
   public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
         ItemStack itemStack = user.getItemInHand(hand);
         user.startUsingItem(hand);
         return InteractionResultHolder.consume(itemStack);
      } else {
         return super.use(world, user, hand);
      }
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
         return UseAnim.BLOCK;
      } else {
         return super.getUseAnimation(stack);
      }
   }

   @Override
   public int getUseDuration(ItemStack stack) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
         return 72000;
      } else {
         return super.getUseDuration(stack);
      }
   }
   public float getDamage() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return this.viaFabricPlus$attackDamage_r1_8;
      } else {
         return this.attackDamage;
      }
      //return this.attackDamage;
   }

   public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      return !pPlayer.isCreative();
   }

   public float getDestroySpeed(ItemStack pStack, BlockState pState) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return (pState.is(Blocks.COBWEB) ? 15.0F : 1.5F);
      }
      if (pState.is(Blocks.COBWEB)) {
         return 15.0F;
      } else {
         return pState.is(BlockTags.SWORD_EFFICIENT) ? 1.5F : 1.0F;
      }
   }

   public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      pStack.hurtAndBreak(1, pAttacker, (p_43296_) -> {
         p_43296_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
      });
      return true;
   }

   public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
      if (pState.getDestroySpeed(pLevel, pPos) != 0.0F) {
         pStack.hurtAndBreak(2, pEntityLiving, (p_43276_) -> {
            p_43276_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
         });
      }

      return true;
   }

   public boolean isCorrectToolForDrops(BlockState pBlock) {
      return pBlock.is(Blocks.COBWEB);
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pEquipmentSlot) {

      return pEquipmentSlot == EquipmentSlot.MAINHAND ?(DebugSettings.global().replaceAttributeModifiers.isEnabled() ? this.viaFabricPlus$AttributeModifiers_r1_8 : this.defaultModifiers ): super.getDefaultAttributeModifiers(pEquipmentSlot);
   }
}