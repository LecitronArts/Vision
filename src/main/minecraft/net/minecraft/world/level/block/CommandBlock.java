package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec((p_312773_) -> {
      return p_312773_.group(Codec.BOOL.fieldOf("automatic").forGetter((p_311238_) -> {
         return p_311238_.automatic;
      }), propertiesCodec()).apply(p_312773_, CommandBlock::new);
   });
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
   private final boolean automatic;

   public MapCodec<CommandBlock> codec() {
      return CODEC;
   }

   public CommandBlock(boolean p_153081_, BlockBehaviour.Properties p_153080_) {
      super(p_153080_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, Boolean.valueOf(false)));
      this.automatic = p_153081_;
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      CommandBlockEntity commandblockentity = new CommandBlockEntity(pPos, pState);
      commandblockentity.setAutomatic(this.automatic);
      return commandblockentity;
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof CommandBlockEntity) {
            CommandBlockEntity commandblockentity = (CommandBlockEntity)blockentity;
            boolean flag = pLevel.hasNeighborSignal(pPos);
            boolean flag1 = commandblockentity.isPowered();
            commandblockentity.setPowered(flag);
            if (!flag1 && !commandblockentity.isAutomatic() && commandblockentity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
               if (flag) {
                  commandblockentity.markConditionMet();
                  pLevel.scheduleTick(pPos, this, 1);
               }

            }
         }
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof CommandBlockEntity commandblockentity) {
         BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
         boolean flag = !StringUtil.isNullOrEmpty(basecommandblock.getCommand());
         CommandBlockEntity.Mode commandblockentity$mode = commandblockentity.getMode();
         boolean flag1 = commandblockentity.wasConditionMet();
         if (commandblockentity$mode == CommandBlockEntity.Mode.AUTO) {
            commandblockentity.markConditionMet();
            if (flag1) {
               this.execute(pState, pLevel, pPos, basecommandblock, flag);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }

            if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
               pLevel.scheduleTick(pPos, this, 1);
            }
         } else if (commandblockentity$mode == CommandBlockEntity.Mode.REDSTONE) {
            if (flag1) {
               this.execute(pState, pLevel, pPos, basecommandblock, flag);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }
         }

         pLevel.updateNeighbourForOutputSignal(pPos, this);
      }

   }

   private void execute(BlockState pState, Level pLevel, BlockPos pPos, BaseCommandBlock pLogic, boolean pCanTrigger) {
      if (pCanTrigger) {
         pLogic.performCommand(pLevel);
      } else {
         pLogic.setSuccessCount(0);
      }

      executeChain(pLevel, pPos, pState.getValue(FACING));
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof CommandBlockEntity && pPlayer.canUseGameMasterBlocks()) {
         pPlayer.openCommandBlock((CommandBlockEntity)blockentity);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      return blockentity instanceof CommandBlockEntity ? ((CommandBlockEntity)blockentity).getCommandBlock().getSuccessCount() : 0;
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof CommandBlockEntity commandblockentity) {
         BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
         if (pStack.hasCustomHoverName()) {
            basecommandblock.setName(pStack.getHoverName());
         }

         if (!pLevel.isClientSide) {
            if (BlockItem.getBlockEntityData(pStack) == null) {
               basecommandblock.setTrackOutput(pLevel.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
               commandblockentity.setAutomatic(this.automatic);
            }

            if (commandblockentity.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
               boolean flag = pLevel.hasNeighborSignal(pPos);
               commandblockentity.setPowered(flag);
            }
         }

      }
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
      pBuilder.add(FACING, CONDITIONAL);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
   }

   private static void executeChain(Level pLevel, BlockPos pPos, Direction pDirection) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
      GameRules gamerules = pLevel.getGameRules();

      int i;
      BlockState blockstate;
      for(i = gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH); i-- > 0; pDirection = blockstate.getValue(FACING)) {
         blockpos$mutableblockpos.move(pDirection);
         blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
         Block block = blockstate.getBlock();
         if (!blockstate.is(Blocks.CHAIN_COMMAND_BLOCK)) {
            break;
         }

         BlockEntity blockentity = pLevel.getBlockEntity(blockpos$mutableblockpos);
         if (!(blockentity instanceof CommandBlockEntity)) {
            break;
         }

         CommandBlockEntity commandblockentity = (CommandBlockEntity)blockentity;
         if (commandblockentity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            break;
         }

         if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
            BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
            if (commandblockentity.markConditionMet()) {
               if (!basecommandblock.performCommand(pLevel)) {
                  break;
               }

               pLevel.updateNeighbourForOutputSignal(blockpos$mutableblockpos, block);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }
         }
      }

      if (i <= 0) {
         int j = Math.max(gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
         LOGGER.warn("Command Block chain tried to execute more than {} steps!", (int)j);
      }

   }
}