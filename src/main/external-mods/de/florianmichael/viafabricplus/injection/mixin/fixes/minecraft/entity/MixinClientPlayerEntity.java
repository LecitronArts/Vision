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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocols.release.protocol1_6_1to1_5_2.Protocol1_6_1to1_5_2;
import net.raphimc.vialegacy.protocols.release.protocol1_6_1to1_5_2.ServerboundPackets1_5_2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("ConstantValue")
@Mixin(value = LocalPlayer.class, priority = 2000)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {

    @Shadow
    public Input input;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    private boolean lastOnGround;

    @Shadow
    private int positionReminder;

    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    @Final
    public ClientPacketListener connection;

/*    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 0))
    private boolean removeVehicleRequirement(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20) && instance.isPassenger();
    }*/

/*
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendIsSprintingIfNeeded()V"))
    private boolean removeSprintingPacket(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_19_3);
    }
*/

/*    @Redirect(method = "updateAutoJump", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;invSqrt(F)F"))
    private float useFastInverseSqrt(float x) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            x = Float.intBitsToFloat(1597463007 - (Float.floatToIntBits(x) >> 1));
            return x * (1.5F - (0.5F * x) * x * x);
        } else {
            return Mth.invSqrt(x);
        }
    }*/

/*    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z"))
    private boolean removeVehicleCheck(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_3) && instance.isPassenger();
    }*/

/*    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isFallFlying()Z"))
    private boolean removeFallFlyingCheck(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_3) && instance.isFallFlying();
    }*/

/*    @Redirect(method = "hasEnoughFoodToStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z"))
    private boolean dontAllowSprintingAsPassenger(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_1) && instance.isPassenger();
    }*/

/*
    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;square(D)D"))
    private double changeMagnitude(double n) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            return 9.0E-4D;
        } else {
            return Mth.square(n);
        }
    }
*/

/*    @Inject(method = "startRiding", at = @At("RETURN"))
    private void setRotationsWhenInBoat(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof Boat && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            this.yRotO = entity.getYRot();
            this.setYRot(entity.getYRot());
            this.setYHeadRot(entity.getYRot());
        }
    }*/

/*    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onClimbable()Z"))
    private boolean allowElytraWhenClimbing(boolean original) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_1) && original;
    }*/

/*
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 3))
    private boolean allowElytraInVehicle(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_4) && instance.isPassenger();
    }
*/

/*
    @ModifyVariable(method = "aiStep", at = @At(value = "LOAD", ordinal = 4), ordinal = 4)
    private boolean removeBl8Boolean(boolean value) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_4) && value;
    }
*/

/*    @Inject(method = "aiStep()V",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;shiftKeyDown:Z", ordinal = 0))
    private void injectTickMovement(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            if (this.input.shiftKeyDown) {
                this.input.leftImpulse = (float) ((double) this.input.leftImpulse / 0.3D);
                this.input.forwardImpulse = (float) ((double) this.input.forwardImpulse / 0.3D);
            }
        }
    }*/

/*    @Inject(method = "hasEnoughImpulseToStartSprinting", at = @At("HEAD"), cancellable = true)
    private void easierUnderwaterSprinting(CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_1)) {
            cir.setReturnValue(((LocalPlayer) (Object) this).input.forwardImpulse >= 0.8);
        }
    }*/

/*    @Redirect(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;hasForwardImpulse()Z", ordinal = 0))
    private boolean disableSprintSneak(Input input) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_1)) {
            return input.forwardImpulse >= 0.8F;
        } else {
            return input.hasForwardImpulse();
        }
    }*/

/*    @Redirect(method = "aiStep",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEnoughImpulseToStartSprinting()Z")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSwimming()Z", ordinal = 0))
    private boolean dontAllowSneakingWhileSwimming(LocalPlayer instance) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_1) && instance.isSwimming();
    }*/

/*    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z"))
    private boolean disableWaterRelatedMovement(LocalPlayer self) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && self.isInWater();
    }*/

/*    @Redirect(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;positionReminder:I", ordinal = 0))
    private int moveLastPosPacketIncrement(LocalPlayer instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return positionReminder - 1; // Reverting original operation
        } else {
            return positionReminder;
        }
    }*/

/*    @Inject(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z"))
    private void moveLastPosPacketIncrement(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            this.positionReminder++;
        }
    }*/

/*    @Redirect(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;lastOnGround:Z", ordinal = 0))
    private boolean sendIdlePacket(LocalPlayer instance) {
        if (DebugSettings.global().sendIdlePacket.isEnabled()) {
            return !onGround();
        } else {
            return lastOnGround;
        }
    }*/

/*    @Redirect(method = "tick",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    private void modifyPositionPacket(ClientPacketListener instance, Packet<?> packet) throws Exception {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_5_2)) {
            final PacketWrapper playerPosition = PacketWrapper.create(ServerboundPackets1_5_2.PLAYER_POSITION_AND_ROTATION, ( this.connection.getConnection()).viaFabricPlus$getUserConnection());
            playerPosition.write(Type.DOUBLE, this.getDeltaMovement().x); // x
            playerPosition.write(Type.DOUBLE, -999.0D); // y
            playerPosition.write(Type.DOUBLE, -999.0D); // stance
            playerPosition.write(Type.DOUBLE, this.getDeltaMovement().z); // z
            playerPosition.write(Type.FLOAT, this.getYRot()); // yaw
            playerPosition.write(Type.FLOAT, this.getXRot()); // pitch
            playerPosition.write(Type.BOOLEAN, this.onGround()); // onGround
            playerPosition.scheduleSendToServer(Protocol1_6_1to1_5_2.class);
            return;
        }
        instance.send(packet);
    }*/

}
