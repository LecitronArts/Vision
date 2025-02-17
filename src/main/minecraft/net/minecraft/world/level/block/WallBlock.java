package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Map;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Unique;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<WallBlock> CODEC = simpleCodec(WallBlock::new);
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST_WALL = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH_WALL = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH_WALL = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST_WALL = BlockStateProperties.WEST_WALL;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Map<BlockState, VoxelShape> shapeByIndex;
   private final Map<BlockState, VoxelShape> collisionShapeByIndex;
   private static final int WALL_WIDTH = 3;
   private static final int WALL_HEIGHT = 14;
   private static final int POST_WIDTH = 4;
   private static final int POST_COVER_WIDTH = 1;
   private static final int WALL_COVER_START = 7;
   private static final int WALL_COVER_END = 9;
   private static final VoxelShape POST_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
   private static final VoxelShape NORTH_TEST = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 9.0D);
   private static final VoxelShape SOUTH_TEST = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 16.0D);
   private static final VoxelShape WEST_TEST = Block.box(0.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
   private static final VoxelShape EAST_TEST = Block.box(7.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
   private final Object2IntMap<BlockState> viaFabricPlus$shapeIndexCache_r1_12_2 = new Object2IntOpenHashMap<>();

   private VoxelShape[] viaFabricPlus$collision_shape_r1_12_2;

   private VoxelShape[] viaFabricPlus$outline_shape_r1_12_2;
   public MapCodec<WallBlock> codec() {
      return CODEC;
   }

   public WallBlock(BlockBehaviour.Properties p_57964_) {
      super(p_57964_);
      this.registerDefaultState(this.stateDefinition.any().setValue(UP, Boolean.valueOf(true)).setValue(NORTH_WALL, WallSide.NONE).setValue(EAST_WALL, WallSide.NONE).setValue(SOUTH_WALL, WallSide.NONE).setValue(WEST_WALL, WallSide.NONE).setValue(WATERLOGGED, Boolean.valueOf(false)));
      this.shapeByIndex = this.makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
      this.collisionShapeByIndex = this.makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
      this.viaFabricPlus$collision_shape_r1_12_2 = this.viaFabricPlus$createShapes1_12_2(24.0F, 24.0F);
      this.viaFabricPlus$outline_shape_r1_12_2 = this.viaFabricPlus$createShapes1_12_2(16.0F, 14.0F);
   }

   private static VoxelShape applyWallShape(VoxelShape pBaseShape, WallSide pHeight, VoxelShape pLowShape, VoxelShape pTallShape) {
      if (pHeight == WallSide.TALL) {
         return Shapes.or(pBaseShape, pTallShape);
      } else {
         return pHeight == WallSide.LOW ? Shapes.or(pBaseShape, pLowShape) : pBaseShape;
      }
   }

   private Map<BlockState, VoxelShape> makeShapes(float pWidth, float pDepth, float pWallPostHeight, float pWallMinY, float pWallLowHeight, float pWallTallHeight) {
      float f = 8.0F - pWidth;
      float f1 = 8.0F + pWidth;
      float f2 = 8.0F - pDepth;
      float f3 = 8.0F + pDepth;
      VoxelShape voxelshape = Block.box((double)f, 0.0D, (double)f, (double)f1, (double)pWallPostHeight, (double)f1);
      VoxelShape voxelshape1 = Block.box((double)f2, (double)pWallMinY, 0.0D, (double)f3, (double)pWallLowHeight, (double)f3);
      VoxelShape voxelshape2 = Block.box((double)f2, (double)pWallMinY, (double)f2, (double)f3, (double)pWallLowHeight, 16.0D);
      VoxelShape voxelshape3 = Block.box(0.0D, (double)pWallMinY, (double)f2, (double)f3, (double)pWallLowHeight, (double)f3);
      VoxelShape voxelshape4 = Block.box((double)f2, (double)pWallMinY, (double)f2, 16.0D, (double)pWallLowHeight, (double)f3);
      VoxelShape voxelshape5 = Block.box((double)f2, (double)pWallMinY, 0.0D, (double)f3, (double)pWallTallHeight, (double)f3);
      VoxelShape voxelshape6 = Block.box((double)f2, (double)pWallMinY, (double)f2, (double)f3, (double)pWallTallHeight, 16.0D);
      VoxelShape voxelshape7 = Block.box(0.0D, (double)pWallMinY, (double)f2, (double)f3, (double)pWallTallHeight, (double)f3);
      VoxelShape voxelshape8 = Block.box((double)f2, (double)pWallMinY, (double)f2, 16.0D, (double)pWallTallHeight, (double)f3);
      ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

      for(Boolean obool : UP.getPossibleValues()) {
         for(WallSide wallside : EAST_WALL.getPossibleValues()) {
            for(WallSide wallside1 : NORTH_WALL.getPossibleValues()) {
               for(WallSide wallside2 : WEST_WALL.getPossibleValues()) {
                  for(WallSide wallside3 : SOUTH_WALL.getPossibleValues()) {
                     VoxelShape voxelshape9 = Shapes.empty();
                     voxelshape9 = applyWallShape(voxelshape9, wallside, voxelshape4, voxelshape8);
                     voxelshape9 = applyWallShape(voxelshape9, wallside2, voxelshape3, voxelshape7);
                     voxelshape9 = applyWallShape(voxelshape9, wallside1, voxelshape1, voxelshape5);
                     voxelshape9 = applyWallShape(voxelshape9, wallside3, voxelshape2, voxelshape6);
                     if (obool) {
                        voxelshape9 = Shapes.or(voxelshape9, voxelshape);
                     }

                     BlockState blockstate = this.defaultBlockState().setValue(UP, obool).setValue(EAST_WALL, wallside).setValue(WEST_WALL, wallside2).setValue(NORTH_WALL, wallside1).setValue(SOUTH_WALL, wallside3);
                     builder.put(blockstate.setValue(WATERLOGGED, Boolean.valueOf(false)), voxelshape9);
                     builder.put(blockstate.setValue(WATERLOGGED, Boolean.valueOf(true)), voxelshape9);
                  }
               }
            }
         }
      }

      return builder.build();
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (pState.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return (this.viaFabricPlus$outline_shape_r1_12_2[this.viaFabricPlus$getShapeIndex(pState)]);
      }
      return this.shapeByIndex.get(pState);
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (pState.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return (this.viaFabricPlus$collision_shape_r1_12_2[this.viaFabricPlus$getShapeIndex(pState)]);
      }
      return this.collisionShapeByIndex.get(pState);
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }

   private boolean connectsTo(BlockState pState, boolean pSideSolid, Direction pDirection) {
      Block block = pState.getBlock();
      boolean flag = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(pState, pDirection);
      return pState.is(BlockTags.WALLS) || !isExceptionForConnection(pState) && pSideSolid || block instanceof IronBarsBlock || flag;
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      LevelReader levelreader = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockPos blockpos1 = blockpos.north();
      BlockPos blockpos2 = blockpos.east();
      BlockPos blockpos3 = blockpos.south();
      BlockPos blockpos4 = blockpos.west();
      BlockPos blockpos5 = blockpos.above();
      BlockState blockstate = levelreader.getBlockState(blockpos1);
      BlockState blockstate1 = levelreader.getBlockState(blockpos2);
      BlockState blockstate2 = levelreader.getBlockState(blockpos3);
      BlockState blockstate3 = levelreader.getBlockState(blockpos4);
      BlockState blockstate4 = levelreader.getBlockState(blockpos5);
      boolean flag = this.connectsTo(blockstate, blockstate.isFaceSturdy(levelreader, blockpos1, Direction.SOUTH), Direction.SOUTH);
      boolean flag1 = this.connectsTo(blockstate1, blockstate1.isFaceSturdy(levelreader, blockpos2, Direction.WEST), Direction.WEST);
      boolean flag2 = this.connectsTo(blockstate2, blockstate2.isFaceSturdy(levelreader, blockpos3, Direction.NORTH), Direction.NORTH);
      boolean flag3 = this.connectsTo(blockstate3, blockstate3.isFaceSturdy(levelreader, blockpos4, Direction.EAST), Direction.EAST);
      BlockState blockstate5 = this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
         return (viaFabricPlus$oldWallPlacementLogic(this.updateShape(levelreader, blockstate5, blockpos5, blockstate4, flag, flag1, flag2, flag3)));
      }
      return this.updateShape(levelreader, blockstate5, blockpos5, blockstate4, flag, flag1, flag2, flag3);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      if (pFacing == Direction.DOWN) {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return (viaFabricPlus$oldWallPlacementLogic(super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos)));
         }
         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      } else {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return (viaFabricPlus$oldWallPlacementLogic(pFacing == Direction.UP ? this.topUpdate(pLevel, pState, pFacingPos, pFacingState) : this.sideUpdate(pLevel, pCurrentPos, pState, pFacingPos, pFacingState, pFacing)));
         }
         return pFacing == Direction.UP ? this.topUpdate(pLevel, pState, pFacingPos, pFacingState) : this.sideUpdate(pLevel, pCurrentPos, pState, pFacingPos, pFacingState, pFacing);
      }
   }

   private static boolean isConnected(BlockState pState, Property<WallSide> pHeightProperty) {
      return pState.getValue(pHeightProperty) != WallSide.NONE;
   }

   private static boolean isCovered(VoxelShape pFirstShape, VoxelShape pSecondShape) {
      return !Shapes.joinIsNotEmpty(pSecondShape, pFirstShape, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(LevelReader pLevel, BlockState pState, BlockPos pPos, BlockState pSecondState) {
      boolean flag = isConnected(pState, NORTH_WALL);
      boolean flag1 = isConnected(pState, EAST_WALL);
      boolean flag2 = isConnected(pState, SOUTH_WALL);
      boolean flag3 = isConnected(pState, WEST_WALL);
      return this.updateShape(pLevel, pState, pPos, pSecondState, flag, flag1, flag2, flag3);
   }

   private BlockState sideUpdate(LevelReader pLevel, BlockPos pFirstPos, BlockState pFirstState, BlockPos pSecondPos, BlockState pSecondState, Direction pDir) {
      Direction direction = pDir.getOpposite();
      boolean flag = pDir == Direction.NORTH ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction) : isConnected(pFirstState, NORTH_WALL);
      boolean flag1 = pDir == Direction.EAST ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction) : isConnected(pFirstState, EAST_WALL);
      boolean flag2 = pDir == Direction.SOUTH ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction) : isConnected(pFirstState, SOUTH_WALL);
      boolean flag3 = pDir == Direction.WEST ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction) : isConnected(pFirstState, WEST_WALL);
      BlockPos blockpos = pFirstPos.above();
      BlockState blockstate = pLevel.getBlockState(blockpos);
      return this.updateShape(pLevel, pFirstState, blockpos, blockstate, flag, flag1, flag2, flag3);
   }

   private BlockState updateShape(LevelReader pLevel, BlockState pState, BlockPos pPos, BlockState pNeighbour, boolean pNorthConnection, boolean pEastConnection, boolean pSouthConnection, boolean pWestConnection) {
      VoxelShape voxelshape = pNeighbour.getCollisionShape(pLevel, pPos).getFaceShape(Direction.DOWN);
      BlockState blockstate = this.updateSides(pState, pNorthConnection, pEastConnection, pSouthConnection, pWestConnection, voxelshape);
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
          return (viaFabricPlus$oldWallPlacementLogic(blockstate.setValue(UP, Boolean.valueOf(this.shouldRaisePost(blockstate, pNeighbour, voxelshape)))));
      }
      return blockstate.setValue(UP, Boolean.valueOf(this.shouldRaisePost(blockstate, pNeighbour, voxelshape)));
   }

   private boolean shouldRaisePost(BlockState pState, BlockState pNeighbour, VoxelShape pShape) {
      boolean flag = pNeighbour.getBlock() instanceof WallBlock && pNeighbour.getValue(UP);
      if (flag) {
         return true;
      } else {
         WallSide wallside = pState.getValue(NORTH_WALL);
         WallSide wallside1 = pState.getValue(SOUTH_WALL);
         WallSide wallside2 = pState.getValue(EAST_WALL);
         WallSide wallside3 = pState.getValue(WEST_WALL);
         boolean flag1 = wallside1 == WallSide.NONE;
         boolean flag2 = wallside3 == WallSide.NONE;
         boolean flag3 = wallside2 == WallSide.NONE;
         boolean flag4 = wallside == WallSide.NONE;
         boolean flag5 = flag4 && flag1 && flag2 && flag3 || flag4 != flag1 || flag2 != flag3;
         if (flag5) {
            return true;
         } else {
            boolean flag6 = wallside == WallSide.TALL && wallside1 == WallSide.TALL || wallside2 == WallSide.TALL && wallside3 == WallSide.TALL;
            if (flag6) {
               return false;
            } else {
               return pNeighbour.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(pShape, POST_TEST);
            }
         }
      }
   }

   private BlockState updateSides(BlockState pState, boolean pNorthConnection, boolean pEastConnection, boolean pSouthConnection, boolean pWestConnection, VoxelShape pWallShape) {
      return pState.setValue(NORTH_WALL, this.makeWallState(pNorthConnection, pWallShape, NORTH_TEST)).setValue(EAST_WALL, this.makeWallState(pEastConnection, pWallShape, EAST_TEST)).setValue(SOUTH_WALL, this.makeWallState(pSouthConnection, pWallShape, SOUTH_TEST)).setValue(WEST_WALL, this.makeWallState(pWestConnection, pWallShape, WEST_TEST));
   }

   private WallSide makeWallState(boolean pAllowConnection, VoxelShape pShape, VoxelShape pNeighbourShape) {
      if (pAllowConnection) {
         return isCovered(pShape, pNeighbourShape) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return !pState.getValue(WATERLOGGED);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(UP, NORTH_WALL, EAST_WALL, WEST_WALL, SOUTH_WALL, WATERLOGGED);
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      switch (pRotation) {
         case CLOCKWISE_180:
            return pState.setValue(NORTH_WALL, pState.getValue(SOUTH_WALL)).setValue(EAST_WALL, pState.getValue(WEST_WALL)).setValue(SOUTH_WALL, pState.getValue(NORTH_WALL)).setValue(WEST_WALL, pState.getValue(EAST_WALL));
         case COUNTERCLOCKWISE_90:
            return pState.setValue(NORTH_WALL, pState.getValue(EAST_WALL)).setValue(EAST_WALL, pState.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, pState.getValue(WEST_WALL)).setValue(WEST_WALL, pState.getValue(NORTH_WALL));
         case CLOCKWISE_90:
            return pState.setValue(NORTH_WALL, pState.getValue(WEST_WALL)).setValue(EAST_WALL, pState.getValue(NORTH_WALL)).setValue(SOUTH_WALL, pState.getValue(EAST_WALL)).setValue(WEST_WALL, pState.getValue(SOUTH_WALL));
         default:
            return pState;
      }
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      switch (pMirror) {
         case LEFT_RIGHT:
            return pState.setValue(NORTH_WALL, pState.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, pState.getValue(NORTH_WALL));
         case FRONT_BACK:
            return pState.setValue(EAST_WALL, pState.getValue(WEST_WALL)).setValue(WEST_WALL, pState.getValue(EAST_WALL));
         default:
            return super.mirror(pState, pMirror);
      }
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      if (state.getValue(WallBlock.UP) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return this.shapeByIndex.get(state);
      } else {
         return super.getOcclusionShape(state, world, pos);
      }
   }

   @Unique
   private VoxelShape[] viaFabricPlus$createShapes1_12_2(final float height1, final float height2) {
      final float f = 4.0F;
      final float g = 12.0F;
      final float h = 5.0F;
      final float i = 11.0F;

      final VoxelShape baseShape = Block.box(f, 0.0D, f, g, height1, g);
      final VoxelShape northShape = Block.box(h, 0.0, 0.0D, i, height2, i);
      final VoxelShape southShape = Block.box(h, 0.0, h, i, height2, 16.0D);
      final VoxelShape westShape = Block.box(0.0D, 0.0, h, i, height2, i);
      final VoxelShape eastShape = Block.box(h, 0.0, h, 16.0D, height2, i);
      final VoxelShape[] voxelShapes = new VoxelShape[]{
              Shapes.empty(),
              Block.box(f, 0.0, h, g, height1, 16.0D),
              Block.box(0.0D, 0.0, f, i, height1, g),
              Block.box(f - 4, 0.0, h - 1, g, height1, 16.0D),
              Block.box(f, 0.0, 0.0D, g, height1, i),
              Shapes.or(southShape, northShape),
              Block.box(f - 4, 0.0, 0.0D, g, height1, i + 1),
              Block.box(f - 4, 0.0, h - 5, g, height1, 16.0D),
              Block.box(h, 0.0, f, 16.0D, height1, g),
              Block.box(h - 1, 0.0, f, 16.0D, height1, g + 4),
              Shapes.or(westShape, eastShape),
              Block.box(h - 5, 0.0, f, 16.0D, height1, g + 4),
              Block.box(f, 0.0, 0.0D, g + 4, height1, i + 1),
              Block.box(f, 0.0, 0.0D, g + 4, height1, i + 5),
              Block.box(h - 5, 0.0, f - 4, 16.0D, height1, g),
              Block.box(0, 0.0, 0, 16.0D, height1, 16.0D)
      };

      for (int j = 0; j < 16; ++j) {
         voxelShapes[j] = Shapes.or(baseShape, voxelShapes[j]);
      }

      return voxelShapes;
   }

   @Unique
   private static BlockState viaFabricPlus$oldWallPlacementLogic(BlockState state) {
      boolean addUp = false;
      if (state.getValue(WallBlock.NORTH_WALL) == WallSide.TALL) {
         state = state.setValue(WallBlock.NORTH_WALL, WallSide.LOW);
         addUp = true;
      }
      if (state.getValue(WallBlock.EAST_WALL) == WallSide.TALL) {
         state = state.setValue(WallBlock.EAST_WALL, WallSide.LOW);
         addUp = true;
      }
      if (state.getValue(WallBlock.SOUTH_WALL) == WallSide.TALL) {
         state = state.setValue(WallBlock.SOUTH_WALL, WallSide.LOW);
         addUp = true;
      }
      if (state.getValue(WallBlock.WEST_WALL) == WallSide.TALL) {
         state = state.setValue(WallBlock.WEST_WALL, WallSide.LOW);
         addUp = true;
      }
      if (addUp) {
         state = state.setValue(WallBlock.UP, true);
      }
      return state;
   }

   @Unique
   private static int viaFabricPlus$getDirectionMask(Direction dir) {
      return 1 << dir.get2DDataValue();
   }

   @Unique
   private int viaFabricPlus$getShapeIndex(BlockState state) {
      return this.viaFabricPlus$shapeIndexCache_r1_12_2.computeIntIfAbsent(state, statex -> {
         int i = 0;
         if (!WallSide.NONE.equals(statex.getValue(WallBlock.NORTH_WALL))) {
            i |= viaFabricPlus$getDirectionMask(Direction.NORTH);
         }

         if (!WallSide.NONE.equals(statex.getValue(WallBlock.EAST_WALL))) {
            i |= viaFabricPlus$getDirectionMask(Direction.EAST);
         }

         if (!WallSide.NONE.equals(statex.getValue(WallBlock.SOUTH_WALL))) {
            i |= viaFabricPlus$getDirectionMask(Direction.SOUTH);
         }

         if (!WallSide.NONE.equals(statex.getValue(WallBlock.WEST_WALL))) {
            i |= viaFabricPlus$getDirectionMask(Direction.WEST);
         }

         return i;
      });
   }


}