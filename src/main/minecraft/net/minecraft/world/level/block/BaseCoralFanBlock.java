package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralFanBlock extends BaseCoralPlantTypeBlock {
   public static final MapCodec<BaseCoralFanBlock> CODEC = simpleCodec(BaseCoralFanBlock::new);
   private static final VoxelShape AABB = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);

   public MapCodec<? extends BaseCoralFanBlock> codec() {
      return CODEC;
   }

   protected BaseCoralFanBlock(BlockBehaviour.Properties p_49106_) {
      super(p_49106_);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AABB;
   }
}