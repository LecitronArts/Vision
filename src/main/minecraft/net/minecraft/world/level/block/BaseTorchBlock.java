package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseTorchBlock extends Block {
   protected static final int AABB_STANDING_OFFSET = 2;
   protected static final VoxelShape AABB = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);

   protected BaseTorchBlock(BlockBehaviour.Properties p_310835_) {
      super(p_310835_);
   }

   protected abstract MapCodec<? extends BaseTorchBlock> codec();

   public VoxelShape getShape(BlockState p_310927_, BlockGetter p_313227_, BlockPos p_311676_, CollisionContext p_310238_) {
      return AABB;
   }

   public BlockState updateShape(BlockState p_311008_, Direction p_310059_, BlockState p_312601_, LevelAccessor p_310966_, BlockPos p_313113_, BlockPos p_312310_) {
      return p_310059_ == Direction.DOWN && !this.canSurvive(p_311008_, p_310966_, p_313113_) ? Blocks.AIR.defaultBlockState() : super.updateShape(p_311008_, p_310059_, p_312601_, p_310966_, p_313113_, p_312310_);
   }

   public boolean canSurvive(BlockState p_309766_, LevelReader p_313035_, BlockPos p_311995_) {
      return canSupportCenter(p_313035_, p_311995_.below(), Direction.UP);
   }
}