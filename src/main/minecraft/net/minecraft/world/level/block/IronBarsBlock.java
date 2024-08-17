package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);
   private VoxelShape[] viaFabricPlus$shape_r1_8;
   public MapCodec<? extends IronBarsBlock> codec() {
      return CODEC;
   }

   protected IronBarsBlock(BlockBehaviour.Properties p_54198_) {
      super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, p_54198_);
      this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, Boolean.valueOf(false)).setValue(EAST, Boolean.valueOf(false)).setValue(SOUTH, Boolean.valueOf(false)).setValue(WEST, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false)));
      final float f = 7.0F;
      final float g = 9.0F;
      final float h = 7.0F;
      final float i = 9.0F;

      final VoxelShape baseShape = Block.box(f, 0.0, f, g, (float) 16.0, g);
      final VoxelShape northShape = Block.box(h, (float) 0.0, 0.0, i, (float) 16.0, i);
      final VoxelShape southShape = Block.box(h, (float) 0.0, h, i, (float) 16.0, 16.0);
      final VoxelShape westShape = Block.box(0.0, (float) 0.0, h, i, (float) 16.0, i);
      final VoxelShape eastShape = Block.box(h, (float) 0.0, h, 16.0, (float) 16.0, i);

      final VoxelShape northEastCornerShape = Shapes.or(northShape, eastShape);
      final VoxelShape southWestCornerShape = Shapes.or(southShape, westShape);

      viaFabricPlus$shape_r1_8 = new VoxelShape[] {
              Shapes.empty(),
              Block.box(h, (float) 0.0, h + 1, i, (float) 16.0, 16.0D), // south
              Block.box(0.0D, (float) 0.0, h, i - 1, (float) 16.0, i), // west
              southWestCornerShape,
              Block.box(h, (float) 0.0, 0.0D, i, (float) 16.0, i - 1), // north
              Shapes.or(southShape, northShape),
              Shapes.or(westShape, northShape),
              Shapes.or(southWestCornerShape, northShape),
              Block.box(h + 1, (float) 0.0, h, 16.0D, (float) 16.0, i), // east
              Shapes.or(southShape, eastShape),
              Shapes.or(westShape, eastShape),
              Shapes.or(southWestCornerShape, eastShape),
              northEastCornerShape,
              Shapes.or(southShape, northEastCornerShape),
              Shapes.or(westShape, northEastCornerShape),
              Shapes.or(southWestCornerShape, northEastCornerShape)
      };

      for (int j = 0; j < 16; ++j) {
         if (j == 1 || j == 2 || j == 4 || j == 8) continue;
         viaFabricPlus$shape_r1_8[j] = Shapes.or(baseShape, viaFabricPlus$shape_r1_8[j]);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockGetter blockgetter = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockPos blockpos1 = blockpos.north();
      BlockPos blockpos2 = blockpos.south();
      BlockPos blockpos3 = blockpos.west();
      BlockPos blockpos4 = blockpos.east();
      BlockState blockstate = blockgetter.getBlockState(blockpos1);
      BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
      BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
      BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
      return this.defaultBlockState().setValue(NORTH, Boolean.valueOf(this.attachsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH)))).setValue(SOUTH, Boolean.valueOf(this.attachsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.NORTH)))).setValue(WEST, Boolean.valueOf(this.attachsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.EAST)))).setValue(EAST, Boolean.valueOf(this.attachsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.WEST)))).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return pFacing.getAxis().isHorizontal() ? pState.setValue(PROPERTY_BY_DIRECTION.get(pFacing), Boolean.valueOf(this.attachsTo(pFacingState, pFacingState.isFaceSturdy(pLevel, pFacingPos, pFacing.getOpposite())))) : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
      return Shapes.empty();
   }

   public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
      if (pAdjacentBlockState.is(this)) {
         if (!pSide.getAxis().isHorizontal()) {
            return true;
         }

         if (pState.getValue(PROPERTY_BY_DIRECTION.get(pSide)) && pAdjacentBlockState.getValue(PROPERTY_BY_DIRECTION.get(pSide.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(pState, pAdjacentBlockState, pSide);
   }

   public final boolean attachsTo(BlockState pState, boolean pSolidSide) {
      return !isExceptionForConnection(pState) && pSolidSide || pState.getBlock() instanceof IronBarsBlock || pState.is(BlockTags.WALLS);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return this.viaFabricPlus$shape_r1_8[this.getAABBIndex(state)];
      } else {
         return super.getShape(state, world, pos, context);
      }
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return this.viaFabricPlus$shape_r1_8[this.getAABBIndex(state)];
      } else {
         return super.getCollisionShape(state, world, pos, context);
      }
   }
}