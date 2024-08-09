package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class FungusBlock extends BushBlock implements BonemealableBlock {
   public static final MapCodec<FungusBlock> CODEC = RecordCodecBuilder.mapCodec((p_309284_) -> {
      return p_309284_.group(ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter((p_309283_) -> {
         return p_309283_.feature;
      }), BuiltInRegistries.BLOCK.byNameCodec().fieldOf("grows_on").forGetter((p_309285_) -> {
         return p_309285_.requiredBlock;
      }), propertiesCodec()).apply(p_309284_, FungusBlock::new);
   });
   protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 9.0D, 12.0D);
   private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4D;
   private final Block requiredBlock;
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   public MapCodec<FungusBlock> codec() {
      return CODEC;
   }

   protected FungusBlock(ResourceKey<ConfiguredFeature<?, ?>> p_259087_, Block p_260223_, BlockBehaviour.Properties p_259749_) {
      super(p_259749_);
      this.feature = p_259087_;
      this.requiredBlock = p_260223_;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.is(BlockTags.NYLIUM) || pState.is(Blocks.MYCELIUM) || pState.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(pState, pLevel, pPos);
   }

   private Optional<? extends Holder<ConfiguredFeature<?, ?>>> getFeature(LevelReader pLevel) {
      return pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(this.feature);
   }

   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      BlockState blockstate = pLevel.getBlockState(pPos.below());
      return blockstate.is(this.requiredBlock);
   }

   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return (double)pRandom.nextFloat() < 0.4D;
   }

   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      this.getFeature(pLevel).ifPresent((p_256352_) -> {
         p_256352_.value().place(pLevel, pLevel.getChunkSource().getGenerator(), pRandom, pPos);
      });
   }
}