package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AmethystClusterBlock extends AmethystBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<AmethystClusterBlock> CODEC = RecordCodecBuilder.mapCodec((p_313213_) -> {
      return p_313213_.group(Codec.FLOAT.fieldOf("height").forGetter((p_313043_) -> {
         return p_313043_.height;
      }), Codec.FLOAT.fieldOf("aabb_offset").forGetter((p_310115_) -> {
         return p_310115_.aabbOffset;
      }), propertiesCodec()).apply(p_313213_, AmethystClusterBlock::new);
   });
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   private final float height;
   private final float aabbOffset;
   protected final VoxelShape northAabb;
   protected final VoxelShape southAabb;
   protected final VoxelShape eastAabb;
   protected final VoxelShape westAabb;
   protected final VoxelShape upAabb;
   protected final VoxelShape downAabb;

   public MapCodec<AmethystClusterBlock> codec() {
      return CODEC;
   }

   public AmethystClusterBlock(float p_313148_, float p_309607_, BlockBehaviour.Properties p_152017_) {
      super(p_152017_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(FACING, Direction.UP));
      this.upAabb = Block.box((double)p_309607_, 0.0D, (double)p_309607_, (double)(16.0F - p_309607_), (double)p_313148_, (double)(16.0F - p_309607_));
      this.downAabb = Block.box((double)p_309607_, (double)(16.0F - p_313148_), (double)p_309607_, (double)(16.0F - p_309607_), 16.0D, (double)(16.0F - p_309607_));
      this.northAabb = Block.box((double)p_309607_, (double)p_309607_, (double)(16.0F - p_313148_), (double)(16.0F - p_309607_), (double)(16.0F - p_309607_), 16.0D);
      this.southAabb = Block.box((double)p_309607_, (double)p_309607_, 0.0D, (double)(16.0F - p_309607_), (double)(16.0F - p_309607_), (double)p_313148_);
      this.eastAabb = Block.box(0.0D, (double)p_309607_, (double)p_309607_, (double)p_313148_, (double)(16.0F - p_309607_), (double)(16.0F - p_309607_));
      this.westAabb = Block.box((double)(16.0F - p_313148_), (double)p_309607_, (double)p_309607_, 16.0D, (double)(16.0F - p_309607_), (double)(16.0F - p_309607_));
      this.height = p_313148_;
      this.aabbOffset = p_309607_;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction direction = pState.getValue(FACING);
      switch (direction) {
         case NORTH:
            return this.northAabb;
         case SOUTH:
            return this.southAabb;
         case EAST:
            return this.eastAabb;
         case WEST:
            return this.westAabb;
         case DOWN:
            return this.downAabb;
         case UP:
         default:
            return this.upAabb;
      }
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      Direction direction = pState.getValue(FACING);
      BlockPos blockpos = pPos.relative(direction.getOpposite());
      return pLevel.getBlockState(blockpos).isFaceSturdy(pLevel, blockpos, direction);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return pDirection == pState.getValue(FACING).getOpposite() && !pState.canSurvive(pLevel, pPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      LevelAccessor levelaccessor = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER)).setValue(FACING, pContext.getClickedFace());
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(WATERLOGGED, FACING);
   }
}