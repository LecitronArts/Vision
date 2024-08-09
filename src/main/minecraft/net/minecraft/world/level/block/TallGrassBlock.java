package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallGrassBlock extends BushBlock implements BonemealableBlock {
   public static final MapCodec<TallGrassBlock> CODEC = simpleCodec(TallGrassBlock::new);
   protected static final float AABB_OFFSET = 6.0F;
   protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

   public MapCodec<TallGrassBlock> codec() {
      return CODEC;
   }

   protected TallGrassBlock(BlockBehaviour.Properties p_57318_) {
      super(p_57318_);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return true;
   }

   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      DoublePlantBlock doubleplantblock = (DoublePlantBlock)(pState.is(Blocks.FERN) ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
      if (doubleplantblock.defaultBlockState().canSurvive(pLevel, pPos) && pLevel.isEmptyBlock(pPos.above())) {
         DoublePlantBlock.placeAt(pLevel, doubleplantblock.defaultBlockState(), pPos, 2);
      }

   }
}