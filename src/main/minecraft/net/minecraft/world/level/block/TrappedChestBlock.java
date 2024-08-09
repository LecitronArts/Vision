package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TrappedChestBlock extends ChestBlock {
   public static final MapCodec<TrappedChestBlock> CODEC = simpleCodec(TrappedChestBlock::new);

   public MapCodec<TrappedChestBlock> codec() {
      return CODEC;
   }

   public TrappedChestBlock(BlockBehaviour.Properties p_57573_) {
      super(p_57573_, () -> {
         return BlockEntityType.TRAPPED_CHEST;
      });
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new TrappedChestBlockEntity(pPos, pState);
   }

   protected Stat<ResourceLocation> getOpenChestStat() {
      return Stats.CUSTOM.get(Stats.TRIGGER_TRAPPED_CHEST);
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return Mth.clamp(ChestBlockEntity.getOpenCount(pBlockAccess, pPos), 0, 15);
   }

   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pSide == Direction.UP ? pBlockState.getSignal(pBlockAccess, pPos, pSide) : 0;
   }
}