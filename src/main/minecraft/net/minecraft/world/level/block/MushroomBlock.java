package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends BushBlock implements BonemealableBlock {
   public static final MapCodec<MushroomBlock> CODEC = RecordCodecBuilder.mapCodec((p_309886_) -> {
      return p_309886_.group(ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter((p_310622_) -> {
         return p_310622_.feature;
      }), propertiesCodec()).apply(p_309886_, MushroomBlock::new);
   });
   protected static final float AABB_OFFSET = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   public MapCodec<MushroomBlock> codec() {
      return CODEC;
   }

   public MushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> p_256049_, BlockBehaviour.Properties p_256027_) {
      super(p_256027_);
      this.feature = p_256049_;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pRandom.nextInt(25) == 0) {
         int i = 5;
         int j = 4;

         for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-4, -1, -4), pPos.offset(4, 1, 4))) {
            if (pLevel.getBlockState(blockpos).is(this)) {
               --i;
               if (i <= 0) {
                  return;
               }
            }
         }

         BlockPos blockpos1 = pPos.offset(pRandom.nextInt(3) - 1, pRandom.nextInt(2) - pRandom.nextInt(2), pRandom.nextInt(3) - 1);

         for(int k = 0; k < 4; ++k) {
            if (pLevel.isEmptyBlock(blockpos1) && pState.canSurvive(pLevel, blockpos1)) {
               pPos = blockpos1;
            }

            blockpos1 = pPos.offset(pRandom.nextInt(3) - 1, pRandom.nextInt(2) - pRandom.nextInt(2), pRandom.nextInt(3) - 1);
         }

         if (pLevel.isEmptyBlock(blockpos1) && pState.canSurvive(pLevel, blockpos1)) {
            pLevel.setBlock(blockpos1, pState, 2);
         }
      }

   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.isSolidRender(pLevel, pPos);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos blockpos = pPos.below();
      BlockState blockstate = pLevel.getBlockState(blockpos);
      if (blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
         return true;
      } else {
         return pLevel.getRawBrightness(pPos, 0) < 13 && this.mayPlaceOn(blockstate, pLevel, blockpos);
      }
   }

   public boolean growMushroom(ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom) {
      Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional = pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(this.feature);
      if (optional.isEmpty()) {
         return false;
      } else {
         pLevel.removeBlock(pPos, false);
         if (optional.get().value().place(pLevel, pLevel.getChunkSource().getGenerator(), pRandom, pPos)) {
            return true;
         } else {
            pLevel.setBlock(pPos, pState, 3);
            return false;
         }
      }
   }

   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return true;
   }

   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return (double)pRandom.nextFloat() < 0.4D;
   }

   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      this.growMushroom(pLevel, pPos, pState, pRandom);
   }
}