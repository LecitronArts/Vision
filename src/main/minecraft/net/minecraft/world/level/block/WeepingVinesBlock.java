package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WeepingVinesBlock extends GrowingPlantHeadBlock {
   public static final MapCodec<WeepingVinesBlock> CODEC = simpleCodec(WeepingVinesBlock::new);
   protected static final VoxelShape SHAPE = Block.box(4.0D, 9.0D, 4.0D, 12.0D, 16.0D, 12.0D);

   public MapCodec<WeepingVinesBlock> codec() {
      return CODEC;
   }

   public WeepingVinesBlock(BlockBehaviour.Properties p_154966_) {
      super(p_154966_, Direction.DOWN, SHAPE, false, 0.1D);
   }

   protected int getBlocksToGrowWhenBonemealed(RandomSource p_222680_) {
      return NetherVines.getBlocksToGrowWhenBonemealed(p_222680_);
   }

   protected Block getBodyBlock() {
      return Blocks.WEEPING_VINES_PLANT;
   }

   protected boolean canGrowInto(BlockState p_154971_) {
      return NetherVines.isValidGrowthState(p_154971_);
   }
}