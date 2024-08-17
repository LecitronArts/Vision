package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Unique;

public class FenceBlock extends CrossCollisionBlock {
   public static final MapCodec<FenceBlock> CODEC = simpleCodec(FenceBlock::new);
   private final VoxelShape[] occlusionByIndex;

   public MapCodec<FenceBlock> codec() {
      return CODEC;
   }
   @Unique
   private static final VoxelShape viaFabricPlus$shape_b1_8_1 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 24.0D, 16.0D);

   @Unique
   private VoxelShape[] viaFabricPlus$collision_shape_r1_4_7;

   @Unique
   private VoxelShape[] viaFabricPlus$outline_shape_r1_4_7;
   public FenceBlock(BlockBehaviour.Properties p_53302_) {
      super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, p_53302_);
      this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, Boolean.valueOf(false)).setValue(EAST, Boolean.valueOf(false)).setValue(SOUTH, Boolean.valueOf(false)).setValue(WEST, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false)));
      this.occlusionByIndex = this.makeShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
      this.viaFabricPlus$collision_shape_r1_4_7 = this.viaFabricPlus$createShapes1_4_7(24.0F);
      this.viaFabricPlus$outline_shape_r1_4_7 = this.viaFabricPlus$createShapes1_4_7(16.0F);
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return this.occlusionByIndex[this.getAABBIndex(pState)];
   }

   public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
      return this.getShape(pState, pReader, pPos, pContext);
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }

   public boolean connectsTo(BlockState pState, boolean pIsSideSolid, Direction pDirection) {
      Block block = pState.getBlock();
      boolean flag = this.isSameFence(pState);
      boolean flag1 = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(pState, pDirection);
      return !isExceptionForConnection(pState) && pIsSideSolid || flag || flag1;
   }

   private boolean isSameFence(BlockState pState) {
      return pState.is(BlockTags.FENCES) && pState.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
         return (InteractionResult.SUCCESS);
      }
      if (pLevel.isClientSide) {
         ItemStack itemstack = pPlayer.getItemInHand(pHand);
         return itemstack.is(Items.LEAD) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      } else {
         return LeadItem.bindPlayerMobs(pPlayer, pLevel, pPos);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockGetter blockgetter = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockPos blockpos1 = blockpos.north();
      BlockPos blockpos2 = blockpos.east();
      BlockPos blockpos3 = blockpos.south();
      BlockPos blockpos4 = blockpos.west();
      BlockState blockstate = blockgetter.getBlockState(blockpos1);
      BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
      BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
      BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
      return super.getStateForPlacement(pContext).setValue(NORTH, Boolean.valueOf(this.connectsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH), Direction.SOUTH))).setValue(EAST, Boolean.valueOf(this.connectsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.WEST), Direction.WEST))).setValue(SOUTH, Boolean.valueOf(this.connectsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.NORTH), Direction.NORTH))).setValue(WEST, Boolean.valueOf(this.connectsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.EAST), Direction.EAST))).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return pFacing.getAxis().getPlane() == Direction.Plane.HORIZONTAL ? pState.setValue(PROPERTY_BY_DIRECTION.get(pFacing), Boolean.valueOf(this.connectsTo(pFacingState, pFacingState.isFaceSturdy(pLevel, pFacingPos, pFacing.getOpposite()), pFacing.getOpposite()))) : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }

   private VoxelShape[] viaFabricPlus$createShapes1_4_7(final float height) {
      final float f = 6.0F;
      final float g = 10.0F;
      final float h = 6.0F;
      final float i = 10.0F;
      final VoxelShape baseShape = Block.box(f, 0.0, f, g, height, g);
      final VoxelShape northShape = Block.box(h, (float) 0.0, 0.0, i, height, i);
      final VoxelShape southShape = Block.box(h, (float) 0.0, h, i, height, 16.0);
      final VoxelShape westShape = Block.box(0.0, (float) 0.0, h, i, height, i);
      final VoxelShape eastShape = Block.box(h, (float) 0.0, h, 16.0, height, i);
      final VoxelShape[] voxelShapes = new VoxelShape[] {
              Shapes.empty(),
              Block.box(f, (float) 0.0, h, g, height, 16.0D),
              Block.box(0.0D, (float) 0.0, f, i, height, g),
              Block.box(f - 6, (float) 0.0, h, g, height, 16.0D),
              Block.box(f, (float) 0.0, 0.0D, g, height, i),

              Shapes.or(southShape, northShape),
              Block.box(f - 6, (float) 0.0, 0.0D, g, height, i),
              Block.box(f - 6, (float) 0.0, h - 5, g, height, 16.0D),
              Block.box(h, (float) 0.0, f, 16.0D, height, g),
              Block.box(h, (float) 0.0, f, 16.0D, height, g + 6),

              Shapes.or(westShape, eastShape),
              Block.box(h - 5, (float) 0.0, f, 16.0D, height, g + 6),
              Block.box(f, (float) 0.0, 0.0D, g + 6, height, i),
              Block.box(f, (float) 0.0, 0.0D, g + 6, height, i + 5),
              Block.box(h - 5, (float) 0.0, f - 6, 16.0D, height, g),
              Block.box(0, (float) 0.0, 0, 16.0D, height, 16.0D)
      };

      for (int j = 0; j < 16; ++j) {
         voxelShapes[j] = Shapes.or(baseShape, voxelShapes[j]);
      }

      return voxelShapes;
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return Shapes.block();
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
         return this.viaFabricPlus$outline_shape_r1_4_7[this.getAABBIndex(state)];
      } else {
         return super.getShape(state, world, pos, context);
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return viaFabricPlus$shape_b1_8_1;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
         return this.viaFabricPlus$collision_shape_r1_4_7[this.getAABBIndex(state)];
      } else {
         return super.getCollisionShape(state, world, pos, context);
      }
   }

}