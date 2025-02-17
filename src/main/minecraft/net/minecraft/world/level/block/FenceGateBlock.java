package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;

import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Unique;

public class FenceGateBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec((p_312817_) -> {
      return p_312817_.group(WoodType.CODEC.fieldOf("wood_type").forGetter((p_311297_) -> {
         return p_311297_.type;
      }), propertiesCodec()).apply(p_312817_, FenceGateBlock::new);
   });

   private static final VoxelShape viaFabricPlus$x_and_z_axis_collision_shape_b1_8_1 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 24.0D, 16.0D);

   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
   protected static final VoxelShape Z_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
   protected static final VoxelShape X_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
   protected static final VoxelShape Z_SHAPE_LOW = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
   protected static final VoxelShape X_SHAPE_LOW = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D);
   protected static final VoxelShape Z_COLLISION_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 24.0D, 10.0D);
   protected static final VoxelShape X_COLLISION_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 24.0D, 16.0D);
   protected static final VoxelShape Z_SUPPORT_SHAPE = Block.box(0.0D, 5.0D, 6.0D, 16.0D, 24.0D, 10.0D);
   protected static final VoxelShape X_SUPPORT_SHAPE = Block.box(6.0D, 5.0D, 0.0D, 10.0D, 24.0D, 16.0D);
   protected static final VoxelShape Z_OCCLUSION_SHAPE = Shapes.or(Block.box(0.0D, 5.0D, 7.0D, 2.0D, 16.0D, 9.0D), Block.box(14.0D, 5.0D, 7.0D, 16.0D, 16.0D, 9.0D));
   protected static final VoxelShape X_OCCLUSION_SHAPE = Shapes.or(Block.box(7.0D, 5.0D, 0.0D, 9.0D, 16.0D, 2.0D), Block.box(7.0D, 5.0D, 14.0D, 9.0D, 16.0D, 16.0D));
   protected static final VoxelShape Z_OCCLUSION_SHAPE_LOW = Shapes.or(Block.box(0.0D, 2.0D, 7.0D, 2.0D, 13.0D, 9.0D), Block.box(14.0D, 2.0D, 7.0D, 16.0D, 13.0D, 9.0D));
   protected static final VoxelShape X_OCCLUSION_SHAPE_LOW = Shapes.or(Block.box(7.0D, 2.0D, 0.0D, 9.0D, 13.0D, 2.0D), Block.box(7.0D, 2.0D, 14.0D, 9.0D, 13.0D, 16.0D));
   private final WoodType type;

   public MapCodec<FenceGateBlock> codec() {
      return CODEC;
   }

   public FenceGateBlock(WoodType p_273340_, BlockBehaviour.Properties p_273352_) {
      super(p_273352_.sound(p_273340_.soundType()));
      this.type = p_273340_;
      this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, Boolean.valueOf(false)).setValue(POWERED, Boolean.valueOf(false)).setValue(IN_WALL, Boolean.valueOf(false)));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (!pState.getValue(FenceGateBlock.IN_WALL) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return (Shapes.block());
      }
      if (pState.getValue(IN_WALL)) {
         return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE_LOW : Z_SHAPE_LOW;
      } else {
         return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
      }
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      Direction.Axis direction$axis = pFacing.getAxis();
      if (pState.getValue(FACING).getClockWise().getAxis() != direction$axis) {
         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      } else {
         boolean flag = this.isWall(pFacingState) || this.isWall(pLevel.getBlockState(pCurrentPos.relative(pFacing.getOpposite())));
         return pState.setValue(IN_WALL, Boolean.valueOf(flag));
      }
   }

   public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      if (pState.getValue(OPEN)) {
         return Shapes.empty();
      } else {
         return pState.getValue(FACING).getAxis() == Direction.Axis.Z ? Z_SUPPORT_SHAPE : X_SUPPORT_SHAPE;
      }
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (!pState.getValue(FenceGateBlock.OPEN) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
          return (viaFabricPlus$x_and_z_axis_collision_shape_b1_8_1);
      }
      if (pState.getValue(OPEN)) {
         return Shapes.empty();
      } else {
         return pState.getValue(FACING).getAxis() == Direction.Axis.Z ? Z_COLLISION_SHAPE : X_COLLISION_SHAPE;
      }
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      if (pState.getValue(IN_WALL)) {
         return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_OCCLUSION_SHAPE_LOW : Z_OCCLUSION_SHAPE_LOW;
      } else {
         return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_OCCLUSION_SHAPE : Z_OCCLUSION_SHAPE;
      }
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      switch (pType) {
         case LAND:
            return pState.getValue(OPEN);
         case WATER:
            return false;
         case AIR:
            return pState.getValue(OPEN);
         default:
            return false;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      boolean flag = level.hasNeighborSignal(blockpos);
      Direction direction = pContext.getHorizontalDirection();
      Direction.Axis direction$axis = direction.getAxis();
      boolean flag1 = direction$axis == Direction.Axis.Z && (this.isWall(level.getBlockState(blockpos.west())) || this.isWall(level.getBlockState(blockpos.east()))) || direction$axis == Direction.Axis.X && (this.isWall(level.getBlockState(blockpos.north())) || this.isWall(level.getBlockState(blockpos.south())));
      return this.defaultBlockState().setValue(FACING, direction).setValue(OPEN, Boolean.valueOf(flag)).setValue(POWERED, Boolean.valueOf(flag)).setValue(IN_WALL, Boolean.valueOf(flag1));
   }

   private boolean isWall(BlockState pState) {
      return pState.is(BlockTags.WALLS);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pState.getValue(OPEN)) {
         pState = pState.setValue(OPEN, Boolean.valueOf(false));
         pLevel.setBlock(pPos, pState, 10);
      } else {
         Direction direction = pPlayer.getDirection();
         if (pState.getValue(FACING) == direction.getOpposite()) {
            pState = pState.setValue(FACING, direction);
         }

         pState = pState.setValue(OPEN, Boolean.valueOf(true));
         pLevel.setBlock(pPos, pState, 10);
      }

      boolean flag = pState.getValue(OPEN);
      pLevel.playSound(pPlayer, pPos, flag ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundSource.BLOCKS, 1.0F, pLevel.getRandom().nextFloat() * 0.1F + 0.9F);
      pLevel.gameEvent(pPlayer, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
      return InteractionResult.sidedSuccess(pLevel.isClientSide);
   }

   public void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
      if (pExplosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK && !pLevel.isClientSide() && !pState.getValue(POWERED)) {
         boolean flag = pState.getValue(OPEN);
         pLevel.setBlockAndUpdate(pPos, pState.setValue(OPEN, Boolean.valueOf(!flag)));
         pLevel.playSound((Player)null, pPos, flag ? this.type.fenceGateClose() : this.type.fenceGateOpen(), SoundSource.BLOCKS, 1.0F, pLevel.getRandom().nextFloat() * 0.1F + 0.9F);
         pLevel.gameEvent(flag ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pPos, GameEvent.Context.of(pState));
      }

      super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean flag = pLevel.hasNeighborSignal(pPos);
         if (pState.getValue(POWERED) != flag) {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)).setValue(OPEN, Boolean.valueOf(flag)), 2);
            if (pState.getValue(OPEN) != flag) {
               pLevel.playSound((Player)null, pPos, flag ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundSource.BLOCKS, 1.0F, pLevel.getRandom().nextFloat() * 0.1F + 0.9F);
               pLevel.gameEvent((Entity)null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
            }
         }

      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, OPEN, POWERED, IN_WALL);
   }

   public static boolean connectsToDirection(BlockState pState, Direction pDirection) {
      return pState.getValue(FACING).getAxis() == pDirection.getClockWise().getAxis();
   }
}