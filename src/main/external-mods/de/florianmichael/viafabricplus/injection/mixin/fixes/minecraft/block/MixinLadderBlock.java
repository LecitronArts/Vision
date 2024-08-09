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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LadderBlock.class)
public abstract class MixinLadderBlock {

    @Unique
    private static final VoxelShape viaFabricPlus$east_shape_r1_8_x = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);

    @Unique
    private static final VoxelShape viaFabricPlus$west_shape_r1_8_x = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    @Unique
    private static final VoxelShape viaFabricPlus$south_shape_r1_8_x = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);

    @Unique
    private static final VoxelShape viaFabricPlus$north_shape_r1_8_x = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void changeOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            switch (state.getValue(LadderBlock.FACING)) {
                case NORTH -> ci.setReturnValue(viaFabricPlus$north_shape_r1_8_x);
                case SOUTH -> ci.setReturnValue(viaFabricPlus$south_shape_r1_8_x);
                case WEST -> ci.setReturnValue(viaFabricPlus$west_shape_r1_8_x);
                default -> ci.setReturnValue(viaFabricPlus$east_shape_r1_8_x);
            }
        }
    }

}
