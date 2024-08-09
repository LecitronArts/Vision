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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(WallBlock.class)
public abstract class MixinWallBlock extends Block {

    @Shadow
    @Final
    private Map<BlockState, VoxelShape> shapeByIndex;

    @Unique
    private final Object2IntMap<BlockState> viaFabricPlus$shapeIndexCache_r1_12_2 = new Object2IntOpenHashMap<>();

    @Unique
    private VoxelShape[] viaFabricPlus$collision_shape_r1_12_2;

    @Unique
    private VoxelShape[] viaFabricPlus$outline_shape_r1_12_2;

    public MixinWallBlock(Properties settings) {
        super(settings);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initShapes1_12_2(Properties settings, CallbackInfo ci) {
        this.viaFabricPlus$collision_shape_r1_12_2 = this.viaFabricPlus$createShapes1_12_2(24.0F, 24.0F);
        this.viaFabricPlus$outline_shape_r1_12_2 = this.viaFabricPlus$createShapes1_12_2(16.0F, 14.0F);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void modifyPlacementState(CallbackInfoReturnable<BlockState> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            cir.setReturnValue(viaFabricPlus$oldWallPlacementLogic(cir.getReturnValue()));
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void modifyBlockState(CallbackInfoReturnable<BlockState> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            cir.setReturnValue(viaFabricPlus$oldWallPlacementLogic(cir.getReturnValue()));
        }
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void changeCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(this.viaFabricPlus$collision_shape_r1_12_2[this.viaFabricPlus$getShapeIndex(state)]);
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void changeOutlineShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(this.viaFabricPlus$outline_shape_r1_12_2[this.viaFabricPlus$getShapeIndex(state)]);
        }
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        if (state.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return this.shapeByIndex.get(state);
        } else {
            return super.getOcclusionShape(state, world, pos);
        }
    }

    @Unique
    private VoxelShape[] viaFabricPlus$createShapes1_12_2(final float height1, final float height2) {
        final float f = 4.0F;
        final float g = 12.0F;
        final float h = 5.0F;
        final float i = 11.0F;

        final VoxelShape baseShape = Block.box(f, 0.0D, f, g, height1, g);
        final VoxelShape northShape = Block.box(h, 0.0, 0.0D, i, height2, i);
        final VoxelShape southShape = Block.box(h, 0.0, h, i, height2, 16.0D);
        final VoxelShape westShape = Block.box(0.0D, 0.0, h, i, height2, i);
        final VoxelShape eastShape = Block.box(h, 0.0, h, 16.0D, height2, i);
        final VoxelShape[] voxelShapes = new VoxelShape[]{
                Shapes.empty(),
                Block.box(f, 0.0, h, g, height1, 16.0D),
                Block.box(0.0D, 0.0, f, i, height1, g),
                Block.box(f - 4, 0.0, h - 1, g, height1, 16.0D),
                Block.box(f, 0.0, 0.0D, g, height1, i),
                Shapes.or(southShape, northShape),
                Block.box(f - 4, 0.0, 0.0D, g, height1, i + 1),
                Block.box(f - 4, 0.0, h - 5, g, height1, 16.0D),
                Block.box(h, 0.0, f, 16.0D, height1, g),
                Block.box(h - 1, 0.0, f, 16.0D, height1, g + 4),
                Shapes.or(westShape, eastShape),
                Block.box(h - 5, 0.0, f, 16.0D, height1, g + 4),
                Block.box(f, 0.0, 0.0D, g + 4, height1, i + 1),
                Block.box(f, 0.0, 0.0D, g + 4, height1, i + 5),
                Block.box(h - 5, 0.0, f - 4, 16.0D, height1, g),
                Block.box(0, 0.0, 0, 16.0D, height1, 16.0D)
        };

        for (int j = 0; j < 16; ++j) {
            voxelShapes[j] = Shapes.or(baseShape, voxelShapes[j]);
        }

        return voxelShapes;
    }

    @Unique
    private static BlockState viaFabricPlus$oldWallPlacementLogic(BlockState state) {
        boolean addUp = false;
        if (state.getValue(WallBlock.NORTH_WALL) == WallSide.TALL) {
            state = state.setValue(WallBlock.NORTH_WALL, WallSide.LOW);
            addUp = true;
        }
        if (state.getValue(WallBlock.EAST_WALL) == WallSide.TALL) {
            state = state.setValue(WallBlock.EAST_WALL, WallSide.LOW);
            addUp = true;
        }
        if (state.getValue(WallBlock.SOUTH_WALL) == WallSide.TALL) {
            state = state.setValue(WallBlock.SOUTH_WALL, WallSide.LOW);
            addUp = true;
        }
        if (state.getValue(WallBlock.WEST_WALL) == WallSide.TALL) {
            state = state.setValue(WallBlock.WEST_WALL, WallSide.LOW);
            addUp = true;
        }
        if (addUp) {
            state = state.setValue(WallBlock.UP, true);
        }
        return state;
    }

    @Unique
    private static int viaFabricPlus$getDirectionMask(Direction dir) {
        return 1 << dir.get2DDataValue();
    }

    @Unique
    private int viaFabricPlus$getShapeIndex(BlockState state) {
        return this.viaFabricPlus$shapeIndexCache_r1_12_2.computeIntIfAbsent(state, statex -> {
            int i = 0;
            if (!WallSide.NONE.equals(statex.getValue(WallBlock.NORTH_WALL))) {
                i |= viaFabricPlus$getDirectionMask(Direction.NORTH);
            }

            if (!WallSide.NONE.equals(statex.getValue(WallBlock.EAST_WALL))) {
                i |= viaFabricPlus$getDirectionMask(Direction.EAST);
            }

            if (!WallSide.NONE.equals(statex.getValue(WallBlock.SOUTH_WALL))) {
                i |= viaFabricPlus$getDirectionMask(Direction.SOUTH);
            }

            if (!WallSide.NONE.equals(statex.getValue(WallBlock.WEST_WALL))) {
                i |= viaFabricPlus$getDirectionMask(Direction.WEST);
            }

            return i;
        });
    }

}
