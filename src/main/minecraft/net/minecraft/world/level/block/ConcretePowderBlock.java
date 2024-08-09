package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
   public static final MapCodec<ConcretePowderBlock> CODEC = RecordCodecBuilder.mapCodec((p_312823_) -> {
      return p_312823_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("concrete").forGetter((p_313163_) -> {
         return p_313163_.concrete;
      }), propertiesCodec()).apply(p_312823_, ConcretePowderBlock::new);
   });
   private final Block concrete;

   public MapCodec<ConcretePowderBlock> codec() {
      return CODEC;
   }

   public ConcretePowderBlock(Block p_52060_, BlockBehaviour.Properties p_52061_) {
      super(p_52061_);
      this.concrete = p_52060_;
   }

   public void onLand(Level pLevel, BlockPos pPos, BlockState pState, BlockState pReplaceableState, FallingBlockEntity pFallingBlock) {
      if (shouldSolidify(pLevel, pPos, pReplaceableState)) {
         pLevel.setBlock(pPos, this.concrete.defaultBlockState(), 3);
      }

   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockGetter blockgetter = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = blockgetter.getBlockState(blockpos);
      return shouldSolidify(blockgetter, blockpos, blockstate) ? this.concrete.defaultBlockState() : super.getStateForPlacement(pContext);
   }

   private static boolean shouldSolidify(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      return canSolidify(pState) || touchesLiquid(pLevel, pPos);
   }

   private static boolean touchesLiquid(BlockGetter pLevel, BlockPos pPos) {
      boolean flag = false;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

      for(Direction direction : Direction.values()) {
         BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
         if (direction != Direction.DOWN || canSolidify(blockstate)) {
            blockpos$mutableblockpos.setWithOffset(pPos, direction);
            blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            if (canSolidify(blockstate) && !blockstate.isFaceSturdy(pLevel, pPos, direction.getOpposite())) {
               flag = true;
               break;
            }
         }
      }

      return flag;
   }

   private static boolean canSolidify(BlockState pState) {
      return pState.getFluidState().is(FluidTags.WATER);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return touchesLiquid(pLevel, pCurrentPos) ? this.concrete.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public int getDustColor(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return pState.getMapColor(pReader, pPos).col;
   }
}