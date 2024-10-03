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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft;

import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    private Entity entity;

/*    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.BEFORE))
    private void onUpdateHeight(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!DebugSettings.global().replaceSneaking.isEnabled() && DebugSettings.global().sneakInstantly.isEnabled()) {
            eyeHeight = eyeHeightOld = focusedEntity.getEyeHeight();
        }
    }*/

  /*    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void onUpdateEyeHeight(CallbackInfo ci) {
        if (this.entity == null) return;

        if (DebugSettings.global().replaceSneaking.isEnabled()) {
            ci.cancel();
            this.eyeHeightOld = this.eyeHeight;

            if (this.entity instanceof Player player && !player.isSleeping()) {
                if (player.isShiftKeyDown()) {
                    eyeHeight = 1.54F;
                } else if (!DebugSettings.global().longSneaking.isEnabled()) {
                    eyeHeight = 1.62F;
                } else if (eyeHeight < 1.62F) {
                    float delta = 1.62F - eyeHeight;
                    delta *= 0.4F;
                    eyeHeight = 1.62F - delta;
                }
            } else {
                eyeHeight = entity.getEyeHeight();
            }
        }
    }*/

}
