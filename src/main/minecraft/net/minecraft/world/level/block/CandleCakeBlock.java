package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CandleCakeBlock extends AbstractCandleBlock {
   public static final MapCodec<CandleCakeBlock> CODEC = RecordCodecBuilder.mapCodec((p_309279_) -> {
      return p_309279_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("candle").forGetter((p_309278_) -> {
         return p_309278_.candleBlock;
      }), propertiesCodec()).apply(p_309279_, CandleCakeBlock::new);
   });
   public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
   protected static final float AABB_OFFSET = 1.0F;
   protected static final VoxelShape CAKE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D);
   protected static final VoxelShape CANDLE_SHAPE = Block.box(7.0D, 8.0D, 7.0D, 9.0D, 14.0D, 9.0D);
   protected static final VoxelShape SHAPE = Shapes.or(CAKE_SHAPE, CANDLE_SHAPE);
   private static final Map<Block, CandleCakeBlock> BY_CANDLE = Maps.newHashMap();
   private static final Iterable<Vec3> PARTICLE_OFFSETS = ImmutableList.of(new Vec3(0.5D, 1.0D, 0.5D));
   private final Block candleBlock;

   public MapCodec<CandleCakeBlock> codec() {
      return CODEC;
   }

   protected CandleCakeBlock(Block p_152859_, BlockBehaviour.Properties p_152860_) {
      super(p_152860_);
      this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.valueOf(false)));
      BY_CANDLE.put(p_152859_, this);
      this.candleBlock = p_152859_;
   }

   protected Iterable<Vec3> getParticleOffsets(BlockState pState) {
      return PARTICLE_OFFSETS;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
         if (candleHit(pHit) && pPlayer.getItemInHand(pHand).isEmpty() && pState.getValue(LIT)) {
            extinguish(pPlayer, pState, pLevel, pPos);
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
         } else {
            InteractionResult interactionresult = CakeBlock.eat(pLevel, pPos, Blocks.CAKE.defaultBlockState(), pPlayer);
            if (interactionresult.consumesAction()) {
               dropResources(pState, pLevel, pPos);
            }

            return interactionresult;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static boolean candleHit(BlockHitResult pHit) {
      return pHit.getLocation().y - (double)pHit.getBlockPos().getY() > 0.5D;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LIT);
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return new ItemStack(Blocks.CAKE);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      return pDirection == Direction.DOWN && !pState.canSurvive(pLevel, pPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return pLevel.getBlockState(pPos.below()).isSolid();
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return CakeBlock.FULL_CAKE_SIGNAL;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }

   public static BlockState byCandle(Block pCandleBlock) {
      return BY_CANDLE.get(pCandleBlock).defaultBlockState();
   }

   public static boolean canLight(BlockState pState) {
      return pState.is(BlockTags.CANDLE_CAKES, (p_152896_) -> {
         return p_152896_.hasProperty(LIT) && !pState.getValue(LIT);
      });
   }
}