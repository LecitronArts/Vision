package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<LightBlock> CODEC = simpleCodec(LightBlock::new);
   public static final int MAX_LEVEL = 15;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final ToIntFunction<BlockState> LIGHT_EMISSION = (p_153701_) -> {
      return p_153701_.getValue(LEVEL);
   };

   public MapCodec<LightBlock> codec() {
      return CODEC;
   }

   public LightBlock(BlockBehaviour.Properties p_153662_) {
      super(p_153662_);
      this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(15)).setValue(WATERLOGGED, Boolean.valueOf(false)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LEVEL, WATERLOGGED);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (!pLevel.isClientSide && pPlayer.canUseGameMasterBlocks()) {
         pLevel.setBlock(pPos, pState.cycle(LEVEL), 2);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return pContext.isHoldingItem(Items.LIGHT) ? Shapes.block() : Shapes.empty();
   }

   public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return true;
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.INVISIBLE;
   }

   public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return 1.0F;
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return setLightOnStack(super.getCloneItemStack(pLevel, pPos, pState), pState.getValue(LEVEL));
   }

   public static ItemStack setLightOnStack(ItemStack pStack, int pLight) {
      if (pLight != 15) {
         CompoundTag compoundtag = new CompoundTag();
         compoundtag.putString(LEVEL.getName(), String.valueOf(pLight));
         pStack.addTagElement("BlockStateTag", compoundtag);
      }

      return pStack;
   }
}