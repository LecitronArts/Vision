package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class CoralFanBlock extends BaseCoralFanBlock {
   public static final MapCodec<CoralFanBlock> CODEC = RecordCodecBuilder.mapCodec((p_312088_) -> {
      return p_312088_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter((p_311032_) -> {
         return p_311032_.deadBlock;
      }), propertiesCodec()).apply(p_312088_, CoralFanBlock::new);
   });
   private final Block deadBlock;

   public MapCodec<CoralFanBlock> codec() {
      return CODEC;
   }

   protected CoralFanBlock(Block p_52151_, BlockBehaviour.Properties p_52152_) {
      super(p_52152_);
      this.deadBlock = p_52151_;
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      this.tryScheduleDieTick(pState, pLevel, pPos);
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!scanForWater(pState, pLevel, pPos)) {
         pLevel.setBlock(pPos, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(false)), 2);
      }

   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         this.tryScheduleDieTick(pState, pLevel, pCurrentPos);
         if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
         }

         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }
}