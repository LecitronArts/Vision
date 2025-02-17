package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public abstract class DiodeBlock extends HorizontalDirectionalBlock {
   protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   protected DiodeBlock(BlockBehaviour.Properties pProperties) {
      super(pProperties);
   }

   protected abstract MapCodec<? extends DiodeBlock> codec();

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos blockpos = pPos.below();
      return this.canSurviveOn(pLevel, blockpos, pLevel.getBlockState(blockpos));
   }

   protected boolean canSurviveOn(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return pState.isFaceSturdy(pLevel, pPos, Direction.UP, SupportType.RIGID);
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!this.isLocked(pLevel, pPos, pState)) {
         boolean flag = pState.getValue(POWERED);
         boolean flag1 = this.shouldTurnOn(pLevel, pPos, pState);
         if (flag && !flag1) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(false)), 2);
         } else if (!flag) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(true)), 2);
            if (!flag1) {
               pLevel.scheduleTick(pPos, this, this.getDelay(pState), TickPriority.VERY_HIGH);
            }
         }

      }
   }

   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getSignal(pBlockAccess, pPos, pSide);
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      if (!pBlockState.getValue(POWERED)) {
         return 0;
      } else {
         return pBlockState.getValue(FACING) == pSide ? this.getOutputSignal(pBlockAccess, pPos, pBlockState) : 0;
      }
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (pState.canSurvive(pLevel, pPos)) {
         this.checkTickOnNeighbor(pLevel, pPos, pState);
      } else {
         BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
         dropResources(pState, pLevel, pPos, blockentity);
         pLevel.removeBlock(pPos, false);

         for(Direction direction : Direction.values()) {
            pLevel.updateNeighborsAt(pPos.relative(direction), this);
         }

      }
   }

   protected void checkTickOnNeighbor(Level pLevel, BlockPos pPos, BlockState pState) {
      if (!this.isLocked(pLevel, pPos, pState)) {
         boolean flag = pState.getValue(POWERED);
         boolean flag1 = this.shouldTurnOn(pLevel, pPos, pState);
         if (flag != flag1 && !pLevel.getBlockTicks().willTickThisTick(pPos, this)) {
            TickPriority tickpriority = TickPriority.HIGH;
            if (this.shouldPrioritize(pLevel, pPos, pState)) {
               tickpriority = TickPriority.EXTREMELY_HIGH;
            } else if (flag) {
               tickpriority = TickPriority.VERY_HIGH;
            }

            pLevel.scheduleTick(pPos, this, this.getDelay(pState), tickpriority);
         }

      }
   }

   public boolean isLocked(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return false;
   }

   protected boolean shouldTurnOn(Level pLevel, BlockPos pPos, BlockState pState) {
      return this.getInputSignal(pLevel, pPos, pState) > 0;
   }

   protected int getInputSignal(Level pLevel, BlockPos pPos, BlockState pState) {
      Direction direction = pState.getValue(FACING);
      BlockPos blockpos = pPos.relative(direction);
      int i = pLevel.getSignal(blockpos, direction);
      if (i >= 15) {
         return i;
      } else {
         BlockState blockstate = pLevel.getBlockState(blockpos);
         return Math.max(i, blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : 0);
      }
   }

   protected int getAlternateSignal(SignalGetter pLevel, BlockPos pPos, BlockState pState) {
      Direction direction = pState.getValue(FACING);
      Direction direction1 = direction.getClockWise();
      Direction direction2 = direction.getCounterClockWise();
      boolean flag = this.sideInputDiodesOnly();
      return Math.max(pLevel.getControlInputSignal(pPos.relative(direction1), direction1, flag), pLevel.getControlInputSignal(pPos.relative(direction2), direction2, flag));
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      if (this.shouldTurnOn(pLevel, pPos, pState)) {
         pLevel.scheduleTick(pPos, this, 1);
      }

   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      this.updateNeighborsInFront(pLevel, pPos, pState);
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (!pIsMoving && !pState.is(pNewState.getBlock())) {
         super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
         this.updateNeighborsInFront(pLevel, pPos, pState);
      }
   }

   protected void updateNeighborsInFront(Level pLevel, BlockPos pPos, BlockState pState) {
      Direction direction = pState.getValue(FACING);
      BlockPos blockpos = pPos.relative(direction.getOpposite());
      pLevel.neighborChanged(blockpos, this, pPos);
      pLevel.updateNeighborsAtExceptFromFacing(blockpos, this, direction);
   }

   protected boolean sideInputDiodesOnly() {
      return false;
   }

   protected int getOutputSignal(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      return 15;
   }

   public static boolean isDiode(BlockState pState) {
      return pState.getBlock() instanceof DiodeBlock;
   }

   public boolean shouldPrioritize(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      Direction direction = pState.getValue(FACING).getOpposite();
      BlockState blockstate = pLevel.getBlockState(pPos.relative(direction));
      return isDiode(blockstate) && blockstate.getValue(FACING) != direction;
   }

   protected abstract int getDelay(BlockState pState);
}