package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredBlock extends Block {
   public static final MapCodec<PoweredBlock> CODEC = simpleCodec(PoweredBlock::new);

   public MapCodec<PoweredBlock> codec() {
      return CODEC;
   }

   public PoweredBlock(BlockBehaviour.Properties p_55206_) {
      super(p_55206_);
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return 15;
   }
}