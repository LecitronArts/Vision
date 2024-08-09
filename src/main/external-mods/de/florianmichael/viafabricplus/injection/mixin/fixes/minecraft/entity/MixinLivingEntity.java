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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.versioned.visual.EntityRidingOffsetsPre1_20_2;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    @Shadow
    protected boolean jumping;

    @Shadow
    protected abstract float getWaterSlowDown();

    @Shadow
    private Optional<BlockPos> lastClimbablePos;

    @Shadow
    protected abstract boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state);

    @Shadow
    public abstract boolean hasEffect(MobEffect effect);

    @Shadow
    private int noJumpDelay;

    public MixinLivingEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "getMyRidingOffset", at = @At("HEAD"), cancellable = true)
    private void getRidingOffset1_20_1(Entity vehicle, CallbackInfoReturnable<Float> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
            cir.setReturnValue((float) EntityRidingOffsetsPre1_20_2.getHeightOffset(this));
        }
    }

    @Redirect(method = "getPassengerRidingPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getPassengerAttachmentPoint(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityDimensions;F)Lorg/joml/Vector3f;"))
    private Vector3f getPassengerRidingPos1_20_1(LivingEntity instance, Entity entity, EntityDimensions entityDimensions, float v) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
            return EntityRidingOffsetsPre1_20_2.getMountedHeightOffset(instance, entity);
        } else {
            return getPassengerAttachmentPoint(entity, entityDimensions, v);
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isControlledByLocalInstance()Z"))
    private boolean allowPlayerToBeMovedByEntityPackets(LivingEntity instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3) || ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return instance.getControllingPassenger() instanceof Player player ? player.isLocalPlayer() : !instance.level().isClientSide;
        } else {
            return instance.isControlledByLocalInstance();
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void preventEntityPush(CallbackInfo ci) {
        if (DebugSettings.global().preventEntityCramming.isEnabled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;cos(D)D", remap = false))
    private double fixCosTable(double a) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            return Mth.cos((float) a);
        } else {
            return Math.cos(a);
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidHeight(Lnet/minecraft/tags/TagKey;)D"))
    private double dontApplyLavaMovement(LivingEntity instance, TagKey<Fluid> tagKey) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return Double.MAX_VALUE;
        } else {
            return instance.getFluidHeight(tagKey);
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean modify1_13LoadedCheck(Level instance, BlockPos blockPos) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            return this.level().hasChunkAt(blockPos) && instance.getChunkSource().hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        } else {
            return this.level().hasChunkAt(blockPos);
        }
    }

    @Redirect(method = "handleRelativeFrictionAndCalculateMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;jumping:Z"))
    private boolean disableJumpOnLadder(LivingEntity self) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_13_2) && jumping;
    }

    @Redirect(method = "travel",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/effect/MobEffects;DOLPHINS_GRACE:Lnet/minecraft/world/effect/MobEffect;")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;horizontalCollision:Z", ordinal = 0))
    private boolean disableClimbing(LivingEntity instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_13_2) && instance.horizontalCollision;
    }

    @ModifyVariable(method = "getFluidFallingAdjustedMovement", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    private boolean modifyMovingDown(boolean movingDown) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_13_2) && movingDown;
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;resetFallDistance()V"))
    private void dontResetLevitationFallDistance(LivingEntity instance) {
        if (this.hasEffect(MobEffects.SLOW_FALLING) || ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2)) {
            instance.resetFallDistance();
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z", ordinal = 0))
    private boolean modifySwimSprintSpeed(LivingEntity instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && instance.isSprinting();
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidHeight(Lnet/minecraft/tags/TagKey;)D"))
    private double redirectFluidHeight(LivingEntity instance, TagKey<Fluid> tagKey) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && tagKey == FluidTags.WATER) {
            if (instance.getFluidHeight(tagKey) > 0) return 1;
        }
        return instance.getFluidHeight(tagKey);
    }

    @Inject(method = "getFluidFallingAdjustedMovement", at = @At("HEAD"), cancellable = true)
    private void modifySwimSprintFallSpeed(double gravity, boolean movingDown, Vec3 velocity, CallbackInfoReturnable<Vec3> ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && !this.isNoGravity()) {
            ci.setReturnValue(new Vec3(velocity.x, velocity.y - 0.02, velocity.z));
        }
    }

    @ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.9F))
    private float modifySwimFriction(float constant) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return this.getWaterSlowDown();
        } else {
            return constant;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;abs(F)F"))
    private float alwaysRotateWhenWalkingBackwards(float value) {
        if (VisualSettings.global().sidewaysBackwardsRunning.isEnabled()) {
            return 0F;
        } else {
            return Mth.abs(value);
        }
    }

    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private static void removeShieldSlotPreference(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_9_3) && stack.is(Items.SHIELD)) {
            cir.setReturnValue(EquipmentSlot.MAINHAND);
        }
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(doubleValue = 0.003D))
    private double modifyVelocityZero(final double constant) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return 0.005D;
        } else {
            return constant;
        }
    }

    @Inject(method = "trapdoorUsableAsLadder", at = @At("HEAD"), cancellable = true)
    private void disableCrawling(CallbackInfoReturnable<Boolean> ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            ci.setReturnValue(false);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modify1_7StepHeight(EntityType<? extends LivingEntity> type, Level world, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            this.setMaxUpStep(0.5F);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void removeJumpDelay1_0(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThan(LegacyProtocolVersion.r1_0_0tor1_0_1)) {
            this.noJumpDelay = 0;
        }
    }

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void allowGappedLadderClimb(CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThan(LegacyProtocolVersion.b1_5tob1_5_2) && !cir.getReturnValueZ() && !this.isSpectator()) {
            final BlockPos blockPos = this.blockPosition().above();
            final BlockState blockState = this.level().getBlockState(blockPos);
            if (blockState.is(BlockTags.CLIMBABLE)) {
                this.lastClimbablePos = Optional.of(blockPos);
                cir.setReturnValue(true);
            } else if (blockState.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockPos, blockState)) {
                this.lastClimbablePos = Optional.of(blockPos);
                cir.setReturnValue(true);
            }
        }
    }

}
