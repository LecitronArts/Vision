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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.block;

import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class MixinAbstractBlock {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void changeMiningSpeedCalculation(BlockState state, Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (DebugSettings.global().legacyMiningSpeeds.isEnabled()) {
            final float hardness = state.getDestroySpeed(world, pos);
            if (hardness == -1.0F) {
                cir.setReturnValue(0.0F);
            } else {
                if (!player.hasCorrectToolForDrops(state)) {
                    cir.setReturnValue(1.0F / hardness / 100F);
                } else {
                    cir.setReturnValue(player.getDestroySpeed(state) / hardness / 30F);
                }
            }
        }
    }

}
