package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;

public class KelpPlantBlock extends GrowingPlantBodyBlock implements LiquidBlockContainer {
   public static final MapCodec<KelpPlantBlock> CODEC = simpleCodec(KelpPlantBlock::new);

   public MapCodec<KelpPlantBlock> codec() {
      return CODEC;
   }

   protected KelpPlantBlock(BlockBehaviour.Properties p_54323_) {
      super(p_54323_, Direction.UP, Shapes.block(), true);
   }

   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.KELP;
   }

   public FluidState getFluidState(BlockState pState) {
      return Fluids.WATER.getSource(false);
   }

   protected boolean canAttachTo(BlockState pState) {
      return this.getHeadBlock().canAttachTo(pState);
   }

   public boolean canPlaceLiquid(@Nullable Player pPlayer, BlockGetter pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
      return false;
   }

   public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
      return false;
   }
}