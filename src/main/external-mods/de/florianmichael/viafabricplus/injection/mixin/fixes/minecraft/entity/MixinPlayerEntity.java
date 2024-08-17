/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("ConstantValue")
@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity {

    @Shadow
    @Final
    private Abilities abilities;

    @Shadow
    public abstract boolean hasCorrectToolForDrops(BlockState state);

    @Shadow
    @Final
    private Inventory inventory;

    @Unique
    private static final EntityDimensions viaFabricPlus$sneaking_dimensions_v1_13_2 = EntityDimensions.scalable(0.6F, 1.65F);

    @Unique
    private static final SoundEvent viaFabricPlus$oof_hurt = SoundEvent.createVariableRangeEvent(new ResourceLocation("viafabricplus", "oof.hurt"));

    @Unique
    public boolean viaFabricPlus$isSprinting;

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

/*    @Redirect(method = "getMaxHeadRotationRelativeToBody", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isBlocking()Z"))
    private boolean dontModifyHeadRotationWhenBlocking(Player instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20_2) && instance.isBlocking();
    }*/

/*    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSpeed(F)V"))
    private void storeSprintingState(CallbackInfo ci) {
        viaFabricPlus$isSprinting = this.isSprinting();
    }*/

/*    @Redirect(method = "getFlyingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z"))
    private boolean useLastSprintingState(Player instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            return viaFabricPlus$isSprinting;
        } else {
            return instance.isSprinting();
        }
    }*/

/*
    @WrapWithCondition(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean dontSwingHand(Player instance, InteractionHand hand) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_2);
    }
*/

/*
    @Redirect(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean allowElytraWhenLevitating(Player instance, MobEffect statusEffect) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_2) && instance.hasEffect(statusEffect);
    }
*/

/*
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void replaceFallFlyingCondition(CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            if (!this.onGround() && this.getDeltaMovement().y < 0D && !this.isFallFlying()) {
                final ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
                if (itemStack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemStack)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }
*/

/*    @ModifyConstant(method = "getStandingEyeHeight", constant = @Constant(floatValue = 1.27f))
    private float modifySneakEyeHeight(float prevEyeHeight) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            return 1.54F;
        } else {
            return prevEyeHeight;
        }
    }*/

/*
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void onUpdatePose(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            final Pose pose;
            if (this.isFallFlying()) {
                pose = Pose.FALL_FLYING;
            } else if (this.isSleeping()) {
                pose = Pose.SLEEPING;
            } else if (this.isSwimming()) {
                pose = Pose.SWIMMING;
            } else if (this.isAutoSpinAttack()) {
                pose = Pose.SPIN_ATTACK;
            } else if (this.isShiftKeyDown() && !this.abilities.flying) {
                pose = Pose.CROUCHING;
            } else {
                pose = Pose.STANDING;
            }
            this.setPose(pose);
            ci.cancel();
        }
    }
*/

/*
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == Pose.CROUCHING) {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                cir.setReturnValue(Player.STANDING_DIMENSIONS);
            } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
                cir.setReturnValue(viaFabricPlus$sneaking_dimensions_v1_13_2);
            }
        }
    }
*/

/*    @Redirect(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;maxUpStep()F"))
    private float modifyStepHeight1_10(Player instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
            return 1.0F;
        } else {
            return instance.maxUpStep();
        }
    }*/

/*    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void removeAttackCooldown(CallbackInfoReturnable<Float> ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            ci.setReturnValue(1F);
        }
    }*/

/*    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void replaceSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (VisualSettings.global().replaceHurtSoundWithOOFSound.isEnabled()) {
            cir.setReturnValue(viaFabricPlus$oof_hurt);
        }
    }*/

/*
    @Inject(method = "getDestroySpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectUtil;hasDigSpeed(Lnet/minecraft/world/entity/LivingEntity;)Z", shift = At.Shift.BEFORE))
    private void changeSpeedCalculation(BlockState block, CallbackInfoReturnable<Float> cir, @Local LocalFloatRef f) {
        final int efficiency = EnchantmentHelper.getBlockEfficiency(this);
        if (efficiency <= 0) return;

        final float speed = this.inventory.getDestroySpeed(block);
        final int effLevel = efficiency * efficiency + 1;
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_4tor1_4_5) && this.hasCorrectToolForDrops(block)) {
            f.set(speed + effLevel);
        } else if (speed > 1F || ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
            if (!this.getMainHandItem().isEmpty()) {
                if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
                    if (speed <= 1.0 && !this.hasCorrectToolForDrops(block)) {
                        f.set(speed + effLevel * 0.08F);
                    } else {
                        f.set(speed + effLevel);
                    }
                }
            }
        }
    }
*/

/*    @Redirect(method = "getDestroySpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean changeSpeedCalculation(Player instance, MobEffect statusEffect, @Local LocalFloatRef f) {
        final boolean hasMiningFatigue = instance.hasEffect(statusEffect);
        if (hasMiningFatigue && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            f.set(f.get() * (1.0F - (this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier() + 1) * 0.2F));
            if (f.get() < 0) f.set(0);
            return false; // disable original code
        }
        return hasMiningFatigue;
    }*/

/*    @Inject(method = "getPickRange", at = @At("RETURN"), cancellable = true)
    private static void modifyReachDistance(boolean creative, CallbackInfoReturnable<Float> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThan(LegacyProtocolVersion.r1_0_0tor1_0_1) && !creative) {
            cir.setReturnValue(4F);
        }
    }*/

}
