package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetherSproutsBlock extends BushBlock {
   public static final MapCodec<NetherSproutsBlock> CODEC = simpleCodec(NetherSproutsBlock::new);
   protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D);

   public MapCodec<NetherSproutsBlock> codec() {
      return CODEC;
   }

   public NetherSproutsBlock(BlockBehaviour.Properties p_54952_) {
      super(p_54952_);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.is(BlockTags.NYLIUM) || pState.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(pState, pLevel, pPos);
   }
}