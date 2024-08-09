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
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronBarsBlock.class)
public abstract class MixinPaneBlock extends CrossCollisionBlock {

    @Unique
    private VoxelShape[] viaFabricPlus$shape_r1_8;

    protected MixinPaneBlock(float radius1, float radius2, float boundingHeight1, float boundingHeight2, float collisionHeight, Properties settings) {
        super(radius1, radius2, boundingHeight1, boundingHeight2, collisionHeight, settings);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initShapes1_8(Properties settings, CallbackInfo ci) {
        final float f = 7.0F;
        final float g = 9.0F;
        final float h = 7.0F;
        final float i = 9.0F;

        final VoxelShape baseShape = Block.box(f, 0.0, f, g, (float) 16.0, g);
        final VoxelShape northShape = Block.box(h, (float) 0.0, 0.0, i, (float) 16.0, i);
        final VoxelShape southShape = Block.box(h, (float) 0.0, h, i, (float) 16.0, 16.0);
        final VoxelShape westShape = Block.box(0.0, (float) 0.0, h, i, (float) 16.0, i);
        final VoxelShape eastShape = Block.box(h, (float) 0.0, h, 16.0, (float) 16.0, i);

        final VoxelShape northEastCornerShape = Shapes.or(northShape, eastShape);
        final VoxelShape southWestCornerShape = Shapes.or(southShape, westShape);

        viaFabricPlus$shape_r1_8 = new VoxelShape[] {
                Shapes.empty(),
                Block.box(h, (float) 0.0, h + 1, i, (float) 16.0, 16.0D), // south
                Block.box(0.0D, (float) 0.0, h, i - 1, (float) 16.0, i), // west
                southWestCornerShape,
                Block.box(h, (float) 0.0, 0.0D, i, (float) 16.0, i - 1), // north
                Shapes.or(southShape, northShape),
                Shapes.or(westShape, northShape),
                Shapes.or(southWestCornerShape, northShape),
                Block.box(h + 1, (float) 0.0, h, 16.0D, (float) 16.0, i), // east
                Shapes.or(southShape, eastShape),
                Shapes.or(westShape, eastShape),
                Shapes.or(southWestCornerShape, eastShape),
                northEastCornerShape,
                Shapes.or(southShape, northEastCornerShape),
                Shapes.or(westShape, northEastCornerShape),
                Shapes.or(southWestCornerShape, northEastCornerShape)
        };

        for (int j = 0; j < 16; ++j) {
            if (j == 1 || j == 2 || j == 4 || j == 8) continue;
            viaFabricPlus$shape_r1_8[j] = Shapes.or(baseShape, viaFabricPlus$shape_r1_8[j]);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return this.viaFabricPlus$shape_r1_8[this.getAABBIndex(state)];
        } else {
            return super.getShape(state, world, pos, context);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return this.viaFabricPlus$shape_r1_8[this.getAABBIndex(state)];
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

}
