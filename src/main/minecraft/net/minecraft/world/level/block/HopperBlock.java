package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.injection.ViaFabricPlusMixinPlugin;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Unique;

public class HopperBlock extends BaseEntityBlock {
   public static final MapCodec<HopperBlock> CODEC = simpleCodec(HopperBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
   public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
   private static final VoxelShape TOP = Block.box(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   private static final VoxelShape FUNNEL = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 10.0D, 12.0D);
   private static final VoxelShape CONVEX_BASE = Shapes.or(FUNNEL, TOP);
   private static final VoxelShape BASE = Shapes.join(CONVEX_BASE, Hopper.INSIDE, BooleanOp.ONLY_FIRST);
   private static final VoxelShape DOWN_SHAPE = Shapes.or(BASE, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D));
   private static final VoxelShape EAST_SHAPE = Shapes.or(BASE, Block.box(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D));
   private static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, Block.box(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D));
   private static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, Block.box(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D));
   private static final VoxelShape WEST_SHAPE = Shapes.or(BASE, Block.box(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D));
   private static final VoxelShape DOWN_INTERACTION_SHAPE = Hopper.INSIDE;
   private static final VoxelShape EAST_INTERACTION_SHAPE = Shapes.or(Hopper.INSIDE, Block.box(12.0D, 8.0D, 6.0D, 16.0D, 10.0D, 10.0D));
   private static final VoxelShape NORTH_INTERACTION_SHAPE = Shapes.or(Hopper.INSIDE, Block.box(6.0D, 8.0D, 0.0D, 10.0D, 10.0D, 4.0D));
   private static final VoxelShape SOUTH_INTERACTION_SHAPE = Shapes.or(Hopper.INSIDE, Block.box(6.0D, 8.0D, 12.0D, 10.0D, 10.0D, 16.0D));
   private static final VoxelShape WEST_INTERACTION_SHAPE = Shapes.or(Hopper.INSIDE, Block.box(0.0D, 8.0D, 6.0D, 4.0D, 10.0D, 10.0D));
   @Unique
   private boolean viaFabricPlus$requireOriginalShape;
   @Unique
   private static final VoxelShape viaFabricPlus$inside_shape_r1_12_2 = Block.box(2.0D, 10.0D, 2.0D, 14.0D, 16.0D, 14.0D);

   @Unique
   private static final VoxelShape viaFabricPlus$hopper_shape_r1_12_2 = Shapes.join(Shapes.block(), viaFabricPlus$inside_shape_r1_12_2, BooleanOp.ONLY_FIRST);

   public MapCodec<HopperBlock> codec() {
      return CODEC;
   }

   public HopperBlock(BlockBehaviour.Properties p_54039_) {
      super(p_54039_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, Boolean.valueOf(true)));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (ViaFabricPlusMixinPlugin.MORE_CULLING_PRESENT && viaFabricPlus$requireOriginalShape) {
         viaFabricPlus$requireOriginalShape = false;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return (viaFabricPlus$hopper_shape_r1_12_2);
      }
      switch ((Direction)pState.getValue(FACING)) {
         case DOWN:
            return DOWN_SHAPE;
         case NORTH:
            return NORTH_SHAPE;
         case SOUTH:
            return SOUTH_SHAPE;
         case WEST:
            return WEST_SHAPE;
         case EAST:
            return EAST_SHAPE;
         default:
            return BASE;
      }
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      // Workaround for https://github.com/ViaVersion/ViaFabricPlus/issues/45
      viaFabricPlus$requireOriginalShape = true;
      return super.getOcclusionShape(state, world, pos);
   }
   public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return (viaFabricPlus$inside_shape_r1_12_2);
      }
      switch ((Direction)pState.getValue(FACING)) {
         case DOWN:
            return DOWN_INTERACTION_SHAPE;
         case NORTH:
            return NORTH_INTERACTION_SHAPE;
         case SOUTH:
            return SOUTH_INTERACTION_SHAPE;
         case WEST:
            return WEST_INTERACTION_SHAPE;
         case EAST:
            return EAST_INTERACTION_SHAPE;
         default:
            return Hopper.INSIDE;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction direction = pContext.getClickedFace().getOpposite();
      return this.defaultBlockState().setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction).setValue(ENABLED, Boolean.valueOf(true));
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new HopperBlockEntity(pPos, pState);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, BlockEntityType.HOPPER, HopperBlockEntity::pushItemsTick);
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      if (pStack.hasCustomHoverName()) {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof HopperBlockEntity) {
            ((HopperBlockEntity)blockentity).setCustomName(pStack.getHoverName());
         }
      }

   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (!pOldState.is(pState.getBlock())) {
         this.checkPoweredState(pLevel, pPos, pState);
      }
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pLevel.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof HopperBlockEntity) {
            pPlayer.openMenu((HopperBlockEntity)blockentity);
            pPlayer.awardStat(Stats.INSPECT_HOPPER);
         }

         return InteractionResult.CONSUME;
      }
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      this.checkPoweredState(pLevel, pPos, pState);
   }

   private void checkPoweredState(Level pLevel, BlockPos pPos, BlockState pState) {
      boolean flag = !pLevel.hasNeighborSignal(pPos);
      if (flag != pState.getValue(ENABLED)) {
         pLevel.setBlock(pPos, pState.setValue(ENABLED, Boolean.valueOf(flag)), 2);
      }

   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
      super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, ENABLED);
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof HopperBlockEntity) {
         HopperBlockEntity.entityInside(pLevel, pPos, pState, pEntity, (HopperBlockEntity)blockentity);
      }

   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }
}