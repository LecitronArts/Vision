package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BasePressurePlateBlock extends Block {
   protected static final VoxelShape PRESSED_AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
   protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
   protected static final AABB TOUCH_AABB = new AABB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.25D, 0.9375D);
   protected final BlockSetType type;

   protected BasePressurePlateBlock(BlockBehaviour.Properties pProperties, BlockSetType pType) {
      super(pProperties.sound(pType.soundType()));
      this.type = pType;
   }

   protected abstract MapCodec<? extends BasePressurePlateBlock> codec();

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.getSignalForState(pState) > 0 ? PRESSED_AABB : AABB;
   }

   protected int getPressedTime() {
      return 20;
   }

   public boolean isPossibleToRespawnInThis(BlockState pState) {
      return true;
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos blockpos = pPos.below();
      return canSupportRigidBlock(pLevel, blockpos) || canSupportCenter(pLevel, blockpos, Direction.UP);
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      int i = this.getSignalForState(pState);
      if (i > 0) {
         this.checkPressed((Entity)null, pLevel, pPos, pState, i);
      }

   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (!pLevel.isClientSide) {
         int i = this.getSignalForState(pState);
         if (i == 0) {
            this.checkPressed(pEntity, pLevel, pPos, pState, i);
         }

      }
   }

   private void checkPressed(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, BlockState pState, int pCurrentSignal) {
      int i = this.getSignalStrength(pLevel, pPos);
      boolean flag = pCurrentSignal > 0;
      boolean flag1 = i > 0;
      if (pCurrentSignal != i) {
         BlockState blockstate = this.setSignalForState(pState, i);
         pLevel.setBlock(pPos, blockstate, 2);
         this.updateNeighbours(pLevel, pPos);
         pLevel.setBlocksDirty(pPos, pState, blockstate);
      }

      if (!flag1 && flag) {
         pLevel.playSound((Player)null, pPos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
         pLevel.gameEvent(pEntity, GameEvent.BLOCK_DEACTIVATE, pPos);
      } else if (flag1 && !flag) {
         pLevel.playSound((Player)null, pPos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
         pLevel.gameEvent(pEntity, GameEvent.BLOCK_ACTIVATE, pPos);
      }

      if (flag1) {
         pLevel.scheduleTick(new BlockPos(pPos), this, this.getPressedTime());
      }

   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (!pIsMoving && !pState.is(pNewState.getBlock())) {
         if (this.getSignalForState(pState) > 0) {
            this.updateNeighbours(pLevel, pPos);
         }

         super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
      }
   }

   protected void updateNeighbours(Level pLevel, BlockPos pPos) {
      pLevel.updateNeighborsAt(pPos, this);
      pLevel.updateNeighborsAt(pPos.below(), this);
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return this.getSignalForState(pBlockState);
   }

   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pSide == Direction.UP ? this.getSignalForState(pBlockState) : 0;
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   protected static int getEntityCount(Level pLevel, AABB pBox, Class<? extends Entity> pEntityClass) {
      return pLevel.getEntitiesOfClass(pEntityClass, pBox, EntitySelector.NO_SPECTATORS.and((p_289691_) -> {
         return !p_289691_.isIgnoringBlockTriggers();
      })).size();
   }

   protected abstract int getSignalStrength(Level pLevel, BlockPos pPos);

   protected abstract int getSignalForState(BlockState pState);

   protected abstract BlockState setSignalForState(BlockState pState, int pSignal);
}