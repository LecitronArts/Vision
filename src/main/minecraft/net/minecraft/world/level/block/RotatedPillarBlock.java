package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class RotatedPillarBlock extends Block {
   public static final MapCodec<RotatedPillarBlock> CODEC = simpleCodec(RotatedPillarBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

   public MapCodec<? extends RotatedPillarBlock> codec() {
      return CODEC;
   }

   public RotatedPillarBlock(BlockBehaviour.Properties p_55926_) {
      super(p_55926_);
      this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y));
   }

   public BlockState rotate(BlockState pState, Rotation pRot) {
      return rotatePillar(pState, pRot);
   }

   public static BlockState rotatePillar(BlockState pState, Rotation pRotation) {
      switch (pRotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Direction.Axis)pState.getValue(AXIS)) {
               case X:
                  return pState.setValue(AXIS, Direction.Axis.Z);
               case Z:
                  return pState.setValue(AXIS, Direction.Axis.X);
               default:
                  return pState;
            }
         default:
            return pState;
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(AXIS);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(AXIS, pContext.getClickedFace().getAxis());
   }
}