package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SoulSandBlock extends Block {
   public static final MapCodec<SoulSandBlock> CODEC = simpleCodec(SoulSandBlock::new);
   protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);
   private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

   public MapCodec<SoulSandBlock> codec() {
      return CODEC;
   }

   public SoulSandBlock(BlockBehaviour.Properties p_56672_) {
      super(p_56672_);
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_13, ProtocolVersion.v1_15_2)) {
         return (Shapes.empty());
      }
      return Shapes.block();
   }

   public VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
      return Shapes.block();
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      BubbleColumnBlock.updateColumn(pLevel, pPos.above(), pState);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pFacing == Direction.UP && pFacingState.is(Blocks.WATER)) {
         pLevel.scheduleTick(pCurrentPos, this, 20);
      }

      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      pLevel.scheduleTick(pPos, this, 20);
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }

   public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return 0.2F;
   }

   @Override
   public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.4D, 1, 0.4D));
      }
   }

   @Override
   public float getSpeedFactor() {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4) ? 1F : super.getSpeedFactor();
   }
}