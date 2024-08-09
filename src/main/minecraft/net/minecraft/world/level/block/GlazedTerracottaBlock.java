package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class GlazedTerracottaBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<GlazedTerracottaBlock> CODEC = simpleCodec(GlazedTerracottaBlock::new);

   public MapCodec<GlazedTerracottaBlock> codec() {
      return CODEC;
   }

   public GlazedTerracottaBlock(BlockBehaviour.Properties p_53677_) {
      super(p_53677_);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
   }
}