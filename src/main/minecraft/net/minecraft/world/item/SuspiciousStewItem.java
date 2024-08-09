package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class SuspiciousStewItem extends Item {
   public static final String EFFECTS_TAG = "effects";
   public static final int DEFAULT_DURATION = 160;

   public SuspiciousStewItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public static void saveMobEffects(ItemStack pStack, List<SuspiciousEffectHolder.EffectEntry> pEffects) {
      CompoundTag compoundtag = pStack.getOrCreateTag();
      SuspiciousEffectHolder.EffectEntry.LIST_CODEC.encodeStart(NbtOps.INSTANCE, pEffects).result().ifPresent((p_298613_) -> {
         compoundtag.put("effects", p_298613_);
      });
   }

   public static void appendMobEffects(ItemStack pStack, List<SuspiciousEffectHolder.EffectEntry> pEffects) {
      CompoundTag compoundtag = pStack.getOrCreateTag();
      List<SuspiciousEffectHolder.EffectEntry> list = new ArrayList<>();
      listPotionEffects(pStack, list::add);
      list.addAll(pEffects);
      SuspiciousEffectHolder.EffectEntry.LIST_CODEC.encodeStart(NbtOps.INSTANCE, list).result().ifPresent((p_299906_) -> {
         compoundtag.put("effects", p_299906_);
      });
   }

   private static void listPotionEffects(ItemStack pStack, Consumer<SuspiciousEffectHolder.EffectEntry> pOutput) {
      CompoundTag compoundtag = pStack.getTag();
      if (compoundtag != null && compoundtag.contains("effects", 9)) {
         SuspiciousEffectHolder.EffectEntry.LIST_CODEC.parse(NbtOps.INSTANCE, compoundtag.getList("effects", 10)).result().ifPresent((p_299369_) -> {
            p_299369_.forEach(pOutput);
         });
      }

   }

   public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
      super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
      if (pIsAdvanced.isCreative()) {
         List<MobEffectInstance> list = new ArrayList<>();
         listPotionEffects(pStack, (p_297468_) -> {
            list.add(p_297468_.createEffectInstance());
         });
         PotionUtils.addPotionTooltip(list, pTooltipComponents, 1.0F, pLevel == null ? 20.0F : pLevel.tickRateManager().tickrate());
      }

   }

   public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
      ItemStack itemstack = super.finishUsingItem(pStack, pLevel, pEntityLiving);
      listPotionEffects(itemstack, (p_300365_) -> {
         pEntityLiving.addEffect(p_300365_.createEffectInstance());
      });
      return pEntityLiving instanceof Player && ((Player)pEntityLiving).getAbilities().instabuild ? itemstack : new ItemStack(Items.BOWL);
   }
}