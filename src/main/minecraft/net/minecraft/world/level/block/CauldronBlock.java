package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.injection.ViaFabricPlusMixinPlugin;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CauldronBlock extends AbstractCauldronBlock {
   public static final MapCodec<CauldronBlock> CODEC = simpleCodec(CauldronBlock::new);
   private static final float RAIN_FILL_CHANCE = 0.05F;
   private static final float POWDER_SNOW_FILL_CHANCE = 0.1F;
   private boolean viaFabricPlus$requireOriginalShape;
   public MapCodec<CauldronBlock> codec() {
      return CODEC;
   }

   public CauldronBlock(BlockBehaviour.Properties p_51403_) {
      super(p_51403_, CauldronInteraction.EMPTY);
   }

   public boolean isFull(BlockState pState) {
      return false;
   }
   private static final VoxelShape viaFabricPlus$shape_r1_12_2 = Shapes.join(
           Shapes.block(),
           Block.box(2.0D, 5.0D, 2.0D, 14.0D, 16.0D, 14.0D),
           BooleanOp.ONLY_FIRST
   );
   @Override
   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      if (ViaFabricPlusMixinPlugin.MORE_CULLING_PRESENT && viaFabricPlus$requireOriginalShape) {
         viaFabricPlus$requireOriginalShape = false;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return viaFabricPlus$shape_r1_12_2;
      }
      return super.getShape(state, world, pos, context);
   }

   @Override
   public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
      // Workaround for https://github.com/ViaVersion/ViaFabricPlus/issues/246
      // MoreCulling is caching the culling shape and doesn't reload it, so we have to force vanilla's shape here.
      viaFabricPlus$requireOriginalShape = true;
      return super.getOcclusionShape(state, world, pos);
   }
   protected static boolean shouldHandlePrecipitation(Level pLevel, Biome.Precipitation pPrecipitation) {
      if (pPrecipitation == Biome.Precipitation.RAIN) {
         return pLevel.getRandom().nextFloat() < 0.05F;
      } else if (pPrecipitation == Biome.Precipitation.SNOW) {
         return pLevel.getRandom().nextFloat() < 0.1F;
      } else {
         return false;
      }
   }

   public void handlePrecipitation(BlockState pState, Level pLevel, BlockPos pPos, Biome.Precipitation pPrecipitation) {
      if (shouldHandlePrecipitation(pLevel, pPrecipitation)) {
         if (pPrecipitation == Biome.Precipitation.RAIN) {
            pLevel.setBlockAndUpdate(pPos, Blocks.WATER_CAULDRON.defaultBlockState());
            pLevel.gameEvent((Entity)null, GameEvent.BLOCK_CHANGE, pPos);
         } else if (pPrecipitation == Biome.Precipitation.SNOW) {
            pLevel.setBlockAndUpdate(pPos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
            pLevel.gameEvent((Entity)null, GameEvent.BLOCK_CHANGE, pPos);
         }

      }
   }

   protected boolean canReceiveStalactiteDrip(Fluid pFluid) {
      return true;
   }

   protected void receiveStalactiteDrip(BlockState pState, Level pLevel, BlockPos pPos, Fluid pFluid) {
      if (pFluid == Fluids.WATER) {
         BlockState blockstate = Blocks.WATER_CAULDRON.defaultBlockState();
         pLevel.setBlockAndUpdate(pPos, blockstate);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(blockstate));
         pLevel.levelEvent(1047, pPos, 0);
      } else if (pFluid == Fluids.LAVA) {
         BlockState blockstate1 = Blocks.LAVA_CAULDRON.defaultBlockState();
         pLevel.setBlockAndUpdate(pPos, blockstate1);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(blockstate1));
         pLevel.levelEvent(1046, pPos, 0);
      }

   }
}