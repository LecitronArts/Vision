package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaterlilyBlock extends BushBlock {
   public static final MapCodec<WaterlilyBlock> CODEC = simpleCodec(WaterlilyBlock::new);
   protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);
   private static final VoxelShape viaFabricPlus$shape_r1_8_x = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.25D, 16.0D);
   public MapCodec<WaterlilyBlock> codec() {
      return CODEC;
   }

   protected WaterlilyBlock(BlockBehaviour.Properties p_58162_) {
      super(p_58162_);
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      super.entityInside(pState, pLevel, pPos, pEntity);
      if (pLevel instanceof ServerLevel && pEntity instanceof Boat) {
         pLevel.destroyBlock(new BlockPos(pPos), true, pEntity);
      }

   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return (viaFabricPlus$shape_r1_8_x);
      }
      return AABB;
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      FluidState fluidstate = pLevel.getFluidState(pPos);
      FluidState fluidstate1 = pLevel.getFluidState(pPos.above());
      return (fluidstate.getType() == Fluids.WATER || pState.getBlock() instanceof IceBlock) && fluidstate1.getType() == Fluids.EMPTY;
   }
}