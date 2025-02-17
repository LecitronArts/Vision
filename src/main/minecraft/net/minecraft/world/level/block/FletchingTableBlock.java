package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FletchingTableBlock extends CraftingTableBlock {
   public static final MapCodec<FletchingTableBlock> CODEC = simpleCodec(FletchingTableBlock::new);

   public MapCodec<FletchingTableBlock> codec() {
      return CODEC;
   }

   protected FletchingTableBlock(BlockBehaviour.Properties p_53499_) {
      super(p_53499_);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      return InteractionResult.PASS;
   }
}