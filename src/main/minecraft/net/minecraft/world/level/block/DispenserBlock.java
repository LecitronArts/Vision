package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class DispenserBlock extends BaseEntityBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<DispenserBlock> CODEC = simpleCodec(DispenserBlock::new);
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
   private static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = Util.make(new Object2ObjectOpenHashMap<>(), (p_52723_) -> {
      p_52723_.defaultReturnValue(new DefaultDispenseItemBehavior());
   });
   private static final int TRIGGER_DURATION = 4;

   public MapCodec<? extends DispenserBlock> codec() {
      return CODEC;
   }

   public static void registerBehavior(ItemLike pItem, DispenseItemBehavior pBehavior) {
      DISPENSER_REGISTRY.put(pItem.asItem(), pBehavior);
   }

   protected DispenserBlock(BlockBehaviour.Properties p_52664_) {
      super(p_52664_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pLevel.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof DispenserBlockEntity) {
            pPlayer.openMenu((DispenserBlockEntity)blockentity);
            if (blockentity instanceof DropperBlockEntity) {
               pPlayer.awardStat(Stats.INSPECT_DROPPER);
            } else {
               pPlayer.awardStat(Stats.INSPECT_DISPENSER);
            }
         }

         return InteractionResult.CONSUME;
      }
   }

   protected void dispenseFrom(ServerLevel pLevel, BlockState pState, BlockPos pPos) {
      DispenserBlockEntity dispenserblockentity = pLevel.getBlockEntity(pPos, BlockEntityType.DISPENSER).orElse((DispenserBlockEntity)null);
      if (dispenserblockentity == null) {
         LOGGER.warn("Ignoring dispensing attempt for Dispenser without matching block entity at {}", (Object)pPos);
      } else {
         BlockSource blocksource = new BlockSource(pLevel, pPos, pState, dispenserblockentity);
         int i = dispenserblockentity.getRandomSlot(pLevel.random);
         if (i < 0) {
            pLevel.levelEvent(1001, pPos, 0);
            pLevel.gameEvent(GameEvent.BLOCK_ACTIVATE, pPos, GameEvent.Context.of(dispenserblockentity.getBlockState()));
         } else {
            ItemStack itemstack = dispenserblockentity.getItem(i);
            DispenseItemBehavior dispenseitembehavior = this.getDispenseMethod(itemstack);
            if (dispenseitembehavior != DispenseItemBehavior.NOOP) {
               dispenserblockentity.setItem(i, dispenseitembehavior.dispense(blocksource, itemstack));
            }

         }
      }
   }

   protected DispenseItemBehavior getDispenseMethod(ItemStack pStack) {
      return DISPENSER_REGISTRY.get(pStack.getItem());
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean flag = pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.above());
      boolean flag1 = pState.getValue(TRIGGERED);
      if (flag && !flag1) {
         pLevel.scheduleTick(pPos, this, 4);
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
      } else if (!flag && flag1) {
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)), 2);
      }

   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.dispenseFrom(pLevel, pState, pPos);
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new DispenserBlockEntity(pPos, pState);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      if (pStack.hasCustomHoverName()) {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof DispenserBlockEntity) {
            ((DispenserBlockEntity)blockentity).setCustomName(pStack.getHoverName());
         }
      }

   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
      super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
   }

   public static Position getDispensePosition(BlockSource pBlockSource) {
      Direction direction = pBlockSource.state().getValue(FACING);
      return pBlockSource.center().add(0.7D * (double)direction.getStepX(), 0.7D * (double)direction.getStepY(), 0.7D * (double)direction.getStepZ());
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, TRIGGERED);
   }
}