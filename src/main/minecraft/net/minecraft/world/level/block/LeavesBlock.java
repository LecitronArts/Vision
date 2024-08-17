package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.OptionalInt;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeavesBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<LeavesBlock> CODEC = simpleCodec(LeavesBlock::new);
   public static final int DECAY_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
   public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int TICK_DELAY = 1;

   public MapCodec<? extends LeavesBlock> codec() {
      return CODEC;
   }

   public LeavesBlock(BlockBehaviour.Properties p_54422_) {
      super(p_54422_);
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, Integer.valueOf(7)).setValue(PERSISTENT, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false)));
   }

   public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14, ProtocolVersion.v1_15_2)) {
         return (super.getBlockSupportShape(pState, pReader, pPos));
      }
      return Shapes.empty();
   }

   public boolean isRandomlyTicking(BlockState pState) {
      return pState.getValue(DISTANCE) == 7 && !pState.getValue(PERSISTENT);
   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (this.decaying(pState)) {
         dropResources(pState, pLevel, pPos);
         pLevel.removeBlock(pPos, false);
      }

   }

   protected boolean decaying(BlockState pState) {
      return !pState.getValue(PERSISTENT) && pState.getValue(DISTANCE) == 7;
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      pLevel.setBlock(pPos, updateDistance(pState, pLevel, pPos), 3);
   }

   public int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return 1;
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      int i = getDistanceAt(pFacingState) + 1;
      if (i != 1 || pState.getValue(DISTANCE) != i) {
         pLevel.scheduleTick(pCurrentPos, this, 1);
      }

      return pState;
   }

   private static BlockState updateDistance(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
      int i = 7;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(Direction direction : Direction.values()) {
         blockpos$mutableblockpos.setWithOffset(pPos, direction);
         i = Math.min(i, getDistanceAt(pLevel.getBlockState(blockpos$mutableblockpos)) + 1);
         if (i == 1) {
            break;
         }
      }

      return pState.setValue(DISTANCE, Integer.valueOf(i));
   }

   private static int getDistanceAt(BlockState pNeighbor) {
      return getOptionalDistanceAt(pNeighbor).orElse(7);
   }

   public static OptionalInt getOptionalDistanceAt(BlockState pState) {
      if (pState.is(BlockTags.LOGS)) {
         return OptionalInt.of(0);
      } else {
         return pState.hasProperty(DISTANCE) ? OptionalInt.of(pState.getValue(DISTANCE)) : OptionalInt.empty();
      }
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.isRainingAt(pPos.above())) {
         if (pRandom.nextInt(15) == 1) {
            BlockPos blockpos = pPos.below();
            BlockState blockstate = pLevel.getBlockState(blockpos);
            if (!blockstate.canOcclude() || !blockstate.isFaceSturdy(pLevel, blockpos, Direction.UP)) {
               ParticleUtils.spawnParticleBelow(pLevel, pPos, pRandom, ParticleTypes.DRIPPING_WATER);
            }
         }
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(DISTANCE, PERSISTENT, WATERLOGGED);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockState blockstate = this.defaultBlockState().setValue(PERSISTENT, Boolean.valueOf(true)).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
      return updateDistance(blockstate, pContext.getLevel(), pContext.getClickedPos());
   }
}