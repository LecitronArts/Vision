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
import de.florianmichael.viafabricplus.injection.access.IEntity;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("ConstantValue")
@Mixin(Entity.class)
public abstract class MixinEntity implements IEntity {

    @Shadow
    private Level level;

    @Shadow
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;

    @Shadow
    private Vec3 position;

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 velocity);

    @Shadow
    protected abstract Vector3f getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor);

    @Unique
    private boolean viaFabricPlus$isInLoadedChunkAndShouldTick;

    @Inject(method = "getMyRidingOffset", at = @At("HEAD"), cancellable = true)
    private void getRidingOffset1_20_1(Entity vehicle, CallbackInfoReturnable<Float> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
            cir.setReturnValue((float) EntityRidingOffsetsPre1_20_2.getHeightOffset((Entity) (Object) this));
        }
    }

    @Redirect(method = "getPassengerRidingPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPassengerAttachmentPoint(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityDimensions;F)Lorg/joml/Vector3f;"))
    private Vector3f getPassengerRidingPos1_20_1(Entity instance, Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
            return EntityRidingOffsetsPre1_20_2.getMountedHeightOffset(instance, passenger);
        } else {
            return getPassengerAttachmentPoint(passenger, dimensions, scaleFactor);
        }
    }

    @Inject(method = "getOnPos", at = @At("HEAD"), cancellable = true)
    private void modifyPosWithYOffset(float offset, CallbackInfoReturnable<BlockPos> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
            int i = Mth.floor(this.position.x);
            int j = Mth.floor(this.position.y - (double) (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18_2) && offset == 1.0E-5F ? 0.2F : offset));
            int k = Mth.floor(this.position.z);
            BlockPos blockPos = new BlockPos(i, j, k);
            if (this.level.getBlockState(blockPos).isAir()) {
                BlockPos downPos = blockPos.below();
                BlockState blockState = this.level.getBlockState(downPos);
                if (blockState.is(BlockTags.FENCES) || blockState.is(BlockTags.WALLS) || blockState.getBlock() instanceof FenceGateBlock) {
                    cir.setReturnValue(downPos);
                    return;
                }
            }

            cir.setReturnValue(blockPos);
        }
    }

    @ModifyConstant(method = "checkInsideBlocks", constant = @Constant(doubleValue = 1.0E-7))
    private double fixBlockCollisionMargin(double constant) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1)) {
            return 1E-3;
        } else {
            return constant;
        }
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void modifyVelocityAffectingPos(CallbackInfoReturnable<BlockPos> cir) {
        final ProtocolVersion target = ProtocolTranslator.getTargetVersion();

        if (target.olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
            cir.setReturnValue(BlockPos.containing(position.x, getBoundingBox().minY - (target.olderThanOrEqualTo(ProtocolVersion.v1_14_4) ? 1 : 0.5000001), position.z));
        }
    }

    @Redirect(method = {"setYRot", "setXRot"}, at = @At(value = "INVOKE", target = "Ljava/lang/Float;isFinite(F)Z"))
    private boolean allowInfiniteValues(float f) {
        return Float.isFinite(f) || ((Object) this instanceof LocalPlayer && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4));
    }

    @ModifyConstant(method = "getInputVector", constant = @Constant(doubleValue = 1E-7))
    private static double fixVelocityEpsilon(double epsilon) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            return 1E-4;
        } else {
            return epsilon;
        }
    }

    @Redirect(method = "collideWithShapes", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 0))
    private static double alwaysSortYXZ(double a) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            return Double.MAX_VALUE;
        } else {
            return Math.abs(a);
        }
    }

    @Inject(method = "calculateViewVector", at = @At("HEAD"), cancellable = true)
    private void revertCalculation(float pitch, float yaw, CallbackInfoReturnable<Vec3> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(Vec3.directionFromRotation(pitch, yaw));
        }
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void cancelSwimming(boolean swimming, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && swimming) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMovementInFluid", at = @At("HEAD"), cancellable = true)
    private void modifyFluidMovementBoundingBox(TagKey<Fluid> fluidTag, double d, CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2)) {
            return;
        }

        AABB box = this.getBoundingBox().inflate(0, -0.4, 0).deflate(0.001);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        if (!this.level.hasChunksAt(minX, minY, minZ, maxX, maxY, maxZ)) {
            cir.setReturnValue(false);
            return;
        }

        double waterHeight = 0;
        boolean foundFluid = false;
        Vec3 pushVec = Vec3.ZERO;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY - 1; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutable.set(x, y, z);
                    FluidState state = this.level.getFluidState(mutable);
                    if (state.is(fluidTag)) {
                        double height = y + state.getHeight(this.level, mutable);
                        if (height >= box.minY - 0.4)
                            waterHeight = Math.max(height - box.minY + 0.4, waterHeight);
                        if (y >= minY && maxY >= height) {
                            foundFluid = true;
                            pushVec = pushVec.add(state.getFlow(this.level, mutable));
                        }
                    }
                }
            }
        }

        if (pushVec.length() > 0) {
            pushVec = pushVec.normalize().scale(0.014);
            this.setDeltaMovement(this.getDeltaMovement().add(pushVec));
        }

        this.fluidHeight.put(fluidTag, waterHeight);
        cir.setReturnValue(foundFluid);
    }

    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void expandHitBox(CallbackInfoReturnable<Float> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            cir.setReturnValue(0.1F);
        }
    }

    @Override
    public boolean viaFabricPlus$isInLoadedChunkAndShouldTick() {
        return this.viaFabricPlus$isInLoadedChunkAndShouldTick || DebugSettings.global().alwaysTickClientPlayer.isEnabled();
    }

    @Override
    public void viaFabricPlus$setInLoadedChunkAndShouldTick(final boolean inLoadedChunkAndShouldTick) {
        this.viaFabricPlus$isInLoadedChunkAndShouldTick = inLoadedChunkAndShouldTick;
    }

}
