package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DoorBlock extends Block {
   public static final MapCodec<DoorBlock> CODEC = RecordCodecBuilder.mapCodec((p_310521_) -> {
      return p_310521_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type), propertiesCodec()).apply(p_310521_, DoorBlock::new);
   });
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   protected static final float AABB_DOOR_THICKNESS = 3.0F;
   protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
   protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
   protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
   private final BlockSetType type;

   public MapCodec<? extends DoorBlock> codec() {
      return CODEC;
   }

   protected DoorBlock(BlockSetType p_272854_, BlockBehaviour.Properties p_273303_) {
      super(p_273303_.sound(p_272854_.soundType()));
      this.type = p_272854_;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.valueOf(false)).setValue(HINGE, DoorHingeSide.LEFT).setValue(POWERED, Boolean.valueOf(false)).setValue(HALF, DoubleBlockHalf.LOWER));
   }

   public BlockSetType type() {
      return this.type;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction direction = pState.getValue(FACING);
      boolean flag = !pState.getValue(OPEN);
      boolean flag1 = pState.getValue(HINGE) == DoorHingeSide.RIGHT;
      VoxelShape voxelshape;
      switch (direction) {
         case SOUTH:
            voxelshape = flag ? SOUTH_AABB : (flag1 ? EAST_AABB : WEST_AABB);
            break;
         case WEST:
            voxelshape = flag ? WEST_AABB : (flag1 ? SOUTH_AABB : NORTH_AABB);
            break;
         case NORTH:
            voxelshape = flag ? NORTH_AABB : (flag1 ? WEST_AABB : EAST_AABB);
            break;
         default:
            voxelshape = flag ? EAST_AABB : (flag1 ? NORTH_AABB : SOUTH_AABB);
      }

      return voxelshape;
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      DoubleBlockHalf doubleblockhalf = pState.getValue(HALF);
      if (pFacing.getAxis() == Direction.Axis.Y && doubleblockhalf == DoubleBlockHalf.LOWER == (pFacing == Direction.UP)) {
         return pFacingState.getBlock() instanceof DoorBlock && pFacingState.getValue(HALF) != doubleblockhalf ? pFacingState.setValue(HALF, doubleblockhalf) : Blocks.AIR.defaultBlockState();
      } else {
         return doubleblockhalf == DoubleBlockHalf.LOWER && pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }

   public void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
      if (pExplosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK && pState.getValue(HALF) == DoubleBlockHalf.LOWER && !pLevel.isClientSide() && this.type.canOpenByWindCharge() && !pState.getValue(POWERED)) {
         this.setOpen((Entity)null, pLevel, pState, pPos, !this.isOpen(pState));
      }

      super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
   }

   public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
      if (!pLevel.isClientSide && (pPlayer.isCreative() || !pPlayer.hasCorrectToolForDrops(pState))) {
         DoublePlantBlock.preventDropFromBottomPart(pLevel, pPos, pState, pPlayer);
      }

      return super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      boolean flag;
      switch (pType) {
         case LAND:
         case AIR:
            flag = pState.getValue(OPEN);
            break;
         case WATER:
            flag = false;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return flag;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockPos blockpos = pContext.getClickedPos();
      Level level = pContext.getLevel();
      if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(pContext)) {
         boolean flag = level.hasNeighborSignal(blockpos) || level.hasNeighborSignal(blockpos.above());
         return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection()).setValue(HINGE, this.getHinge(pContext)).setValue(POWERED, Boolean.valueOf(flag)).setValue(OPEN, Boolean.valueOf(flag)).setValue(HALF, DoubleBlockHalf.LOWER);
      } else {
         return null;
      }
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      pLevel.setBlock(pPos.above(), pState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   private DoorHingeSide getHinge(BlockPlaceContext pContext) {
      BlockGetter blockgetter = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      Direction direction = pContext.getHorizontalDirection();
      BlockPos blockpos1 = blockpos.above();
      Direction direction1 = direction.getCounterClockWise();
      BlockPos blockpos2 = blockpos.relative(direction1);
      BlockState blockstate = blockgetter.getBlockState(blockpos2);
      BlockPos blockpos3 = blockpos1.relative(direction1);
      BlockState blockstate1 = blockgetter.getBlockState(blockpos3);
      Direction direction2 = direction.getClockWise();
      BlockPos blockpos4 = blockpos.relative(direction2);
      BlockState blockstate2 = blockgetter.getBlockState(blockpos4);
      BlockPos blockpos5 = blockpos1.relative(direction2);
      BlockState blockstate3 = blockgetter.getBlockState(blockpos5);
      int i = (blockstate.isCollisionShapeFullBlock(blockgetter, blockpos2) ? -1 : 0) + (blockstate1.isCollisionShapeFullBlock(blockgetter, blockpos3) ? -1 : 0) + (blockstate2.isCollisionShapeFullBlock(blockgetter, blockpos4) ? 1 : 0) + (blockstate3.isCollisionShapeFullBlock(blockgetter, blockpos5) ? 1 : 0);
      boolean flag = blockstate.is(this) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER;
      boolean flag1 = blockstate2.is(this) && blockstate2.getValue(HALF) == DoubleBlockHalf.LOWER;
      if ((!flag || flag1) && i <= 0) {
         if ((!flag1 || flag) && i >= 0) {
            int j = direction.getStepX();
            int k = direction.getStepZ();
            Vec3 vec3 = pContext.getClickLocation();
            double d0 = vec3.x - (double)blockpos.getX();
            double d1 = vec3.z - (double)blockpos.getZ();
            return (j >= 0 || !(d1 < 0.5D)) && (j <= 0 || !(d1 > 0.5D)) && (k >= 0 || !(d0 > 0.5D)) && (k <= 0 || !(d0 < 0.5D)) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
         } else {
            return DoorHingeSide.LEFT;
         }
      } else {
         return DoorHingeSide.RIGHT;
      }
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (!this.type.canOpenByHand()) {
         return InteractionResult.PASS;
      } else {
         pState = pState.cycle(OPEN);
         pLevel.setBlock(pPos, pState, 10);
         this.playSound(pPlayer, pLevel, pPos, pState.getValue(OPEN));
         pLevel.gameEvent(pPlayer, this.isOpen(pState) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   public boolean isOpen(BlockState pState) {
      return pState.getValue(OPEN);
   }

   public void setOpen(@Nullable Entity pEntity, Level pLevel, BlockState pState, BlockPos pPos, boolean pOpen) {
      if (pState.is(this) && pState.getValue(OPEN) != pOpen) {
         pLevel.setBlock(pPos, pState.setValue(OPEN, Boolean.valueOf(pOpen)), 10);
         this.playSound(pEntity, pLevel, pPos, pOpen);
         pLevel.gameEvent(pEntity, pOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
      }
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean flag = pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.relative(pState.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
      if (!this.defaultBlockState().is(pBlock) && flag != pState.getValue(POWERED)) {
         if (flag != pState.getValue(OPEN)) {
            this.playSound((Entity)null, pLevel, pPos, flag);
            pLevel.gameEvent((Entity)null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
         }

         pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)).setValue(OPEN, Boolean.valueOf(flag)), 2);
      }

   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos blockpos = pPos.below();
      BlockState blockstate = pLevel.getBlockState(blockpos);
      return pState.getValue(HALF) == DoubleBlockHalf.LOWER ? blockstate.isFaceSturdy(pLevel, blockpos, Direction.UP) : blockstate.is(this);
   }

   private void playSound(@Nullable Entity pSource, Level pLevel, BlockPos pPos, boolean pIsOpening) {
      pLevel.playSound(pSource, pPos, pIsOpening ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, pLevel.getRandom().nextFloat() * 0.1F + 0.9F);
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pMirror == Mirror.NONE ? pState : pState.rotate(pMirror.getRotation(pState.getValue(FACING))).cycle(HINGE);
   }

   public long getSeed(BlockState pState, BlockPos pPos) {
      return Mth.getSeed(pPos.getX(), pPos.below(pState.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pPos.getZ());
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(HALF, FACING, OPEN, HINGE, POWERED);
   }

   public static boolean isWoodenDoor(Level pLevel, BlockPos pPos) {
      return isWoodenDoor(pLevel.getBlockState(pPos));
   }

   public static boolean isWoodenDoor(BlockState pState) {
      Block block = pState.getBlock();
      if (block instanceof DoorBlock doorblock) {
         if (doorblock.type().canOpenByHand()) {
            return true;
         }
      }

      return false;
   }
}