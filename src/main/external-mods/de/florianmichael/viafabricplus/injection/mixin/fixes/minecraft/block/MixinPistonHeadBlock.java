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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHeadBlock.class)
public abstract class MixinPistonHeadBlock extends DirectionalBlock {

    @Shadow
    @Final
    protected static VoxelShape DOWN_AABB;

    @Shadow
    @Final
    protected static VoxelShape UP_AABB;

    @Shadow
    @Final
    protected static VoxelShape NORTH_AABB;

    @Shadow
    @Final
    protected static VoxelShape SOUTH_AABB;

    @Shadow
    @Final
    protected static VoxelShape WEST_AABB;

    @Shadow
    @Final
    protected static VoxelShape EAST_AABB;

    @Unique
    private static final VoxelShape viaFabricPlus$up_arm_shape_r1_8_x = Block.box(6.0, 0.0, 6.0, 10.0, 12.0, 10.0);

    @Unique
    private static final VoxelShape viaFabricPlus$down_arm_shape_r1_8_x = Block.box(6.0, 4.0, 6.0, 10.0, 16.0, 10.0);

    @Unique
    private static final VoxelShape viaFabricPlus$south_arm_shape_r1_8_x = Block.box(4.0, 6.0, 0.0, 12.0, 10.0, 12.0);

    @Unique
    private static final VoxelShape viaFabricPlus$north_arm_shape_r1_8_x = Block.box(4.0, 6.0, 4.0, 12.0, 10.0, 16.0);

    @Unique
    private static final VoxelShape viaFabricPlus$east_arm_shape_r1_8_x = Block.box(0.0, 6.0, 4.0, 12.0, 10.0, 12.0);

    @Unique
    private static final VoxelShape viaFabricPlus$west_arm_shape_r1_8_x = Block.box(6.0, 4.0, 4.0, 10.0, 12.0, 16.0);

    @Unique
    private boolean viaFabricPlus$selfInflicted = false;

    protected MixinPistonHeadBlock(Properties settings) {
        super(settings);
    }

/*    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void changeOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (viaFabricPlus$selfInflicted) {
            viaFabricPlus$selfInflicted = false;
            return;
        }
        // Outline shape for piston head doesn't exist in <= 1.12.2
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(switch (state.getValue(PistonHeadBlock.FACING)) {
                case DOWN -> DOWN_AABB;
                case UP -> UP_AABB;
                case NORTH -> NORTH_AABB;
                case SOUTH -> SOUTH_AABB;
                case WEST -> WEST_AABB;
                case EAST -> EAST_AABB;
            });
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return switch (state.getValue(PistonHeadBlock.FACING)) {
                case DOWN -> Shapes.or(DOWN_AABB, viaFabricPlus$down_arm_shape_r1_8_x);
                case UP -> Shapes.or(UP_AABB, viaFabricPlus$up_arm_shape_r1_8_x);
                case NORTH -> Shapes.or(NORTH_AABB, viaFabricPlus$north_arm_shape_r1_8_x);
                case SOUTH -> Shapes.or(SOUTH_AABB, viaFabricPlus$south_arm_shape_r1_8_x);
                case WEST -> Shapes.or(WEST_AABB, viaFabricPlus$west_arm_shape_r1_8_x);
                case EAST -> Shapes.or(EAST_AABB, viaFabricPlus$east_arm_shape_r1_8_x);
            };
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            // Collision shape for piston head in <= 1.12.2 needs to be the 1.13+ outline shape
            viaFabricPlus$selfInflicted = true;
            return getShape(state, world, pos, context);
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }*/

}
