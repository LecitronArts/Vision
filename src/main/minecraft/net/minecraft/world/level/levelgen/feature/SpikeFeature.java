package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.phys.AABB;

public class SpikeFeature extends Feature<SpikeConfiguration> {
   public static final int NUMBER_OF_SPIKES = 10;
   private static final int SPIKE_DISTANCE = 42;
   private static final LoadingCache<Long, List<SpikeFeature.EndSpike>> SPIKE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build(new SpikeFeature.SpikeCacheLoader());

   public SpikeFeature(Codec<SpikeConfiguration> pCodec) {
      super(pCodec);
   }

   public static List<SpikeFeature.EndSpike> getSpikesForLevel(WorldGenLevel pLevel) {
      RandomSource randomsource = RandomSource.create(pLevel.getSeed());
      long i = randomsource.nextLong() & 65535L;
      return SPIKE_CACHE.getUnchecked(i);
   }

   public boolean place(FeaturePlaceContext<SpikeConfiguration> pContext) {
      SpikeConfiguration spikeconfiguration = pContext.config();
      WorldGenLevel worldgenlevel = pContext.level();
      RandomSource randomsource = pContext.random();
      BlockPos blockpos = pContext.origin();
      List<SpikeFeature.EndSpike> list = spikeconfiguration.getSpikes();
      if (list.isEmpty()) {
         list = getSpikesForLevel(worldgenlevel);
      }

      for(SpikeFeature.EndSpike spikefeature$endspike : list) {
         if (spikefeature$endspike.isCenterWithinChunk(blockpos)) {
            this.placeSpike(worldgenlevel, randomsource, spikeconfiguration, spikefeature$endspike);
         }
      }

      return true;
   }

   private void placeSpike(ServerLevelAccessor pLevel, RandomSource pRandom, SpikeConfiguration pConfig, SpikeFeature.EndSpike pSpike) {
      int i = pSpike.getRadius();

      for(BlockPos blockpos : BlockPos.betweenClosed(new BlockPos(pSpike.getCenterX() - i, pLevel.getMinBuildHeight(), pSpike.getCenterZ() - i), new BlockPos(pSpike.getCenterX() + i, pSpike.getHeight() + 10, pSpike.getCenterZ() + i))) {
         if (blockpos.distToLowCornerSqr((double)pSpike.getCenterX(), (double)blockpos.getY(), (double)pSpike.getCenterZ()) <= (double)(i * i + 1) && blockpos.getY() < pSpike.getHeight()) {
            this.setBlock(pLevel, blockpos, Blocks.OBSIDIAN.defaultBlockState());
         } else if (blockpos.getY() > 65) {
            this.setBlock(pLevel, blockpos, Blocks.AIR.defaultBlockState());
         }
      }

      if (pSpike.isGuarded()) {
         int j1 = -2;
         int k1 = 2;
         int j = 3;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(int k = -2; k <= 2; ++k) {
            for(int l = -2; l <= 2; ++l) {
               for(int i1 = 0; i1 <= 3; ++i1) {
                  boolean flag = Mth.abs(k) == 2;
                  boolean flag1 = Mth.abs(l) == 2;
                  boolean flag2 = i1 == 3;
                  if (flag || flag1 || flag2) {
                     boolean flag3 = k == -2 || k == 2 || flag2;
                     boolean flag4 = l == -2 || l == 2 || flag2;
                     BlockState blockstate = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(flag3 && l != -2)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(flag3 && l != 2)).setValue(IronBarsBlock.WEST, Boolean.valueOf(flag4 && k != -2)).setValue(IronBarsBlock.EAST, Boolean.valueOf(flag4 && k != 2));
                     this.setBlock(pLevel, blockpos$mutableblockpos.set(pSpike.getCenterX() + k, pSpike.getHeight() + i1, pSpike.getCenterZ() + l), blockstate);
                  }
               }
            }
         }
      }

      EndCrystal endcrystal = EntityType.END_CRYSTAL.create(pLevel.getLevel());
      if (endcrystal != null) {
         endcrystal.setBeamTarget(pConfig.getCrystalBeamTarget());
         endcrystal.setInvulnerable(pConfig.isCrystalInvulnerable());
         endcrystal.moveTo((double)pSpike.getCenterX() + 0.5D, (double)(pSpike.getHeight() + 1), (double)pSpike.getCenterZ() + 0.5D, pRandom.nextFloat() * 360.0F, 0.0F);
         pLevel.addFreshEntity(endcrystal);
         BlockPos blockpos1 = endcrystal.blockPosition();
         this.setBlock(pLevel, blockpos1.below(), Blocks.BEDROCK.defaultBlockState());
         this.setBlock(pLevel, blockpos1, FireBlock.getState(pLevel, blockpos1));
      }

   }

   public static class EndSpike {
      public static final Codec<SpikeFeature.EndSpike> CODEC = RecordCodecBuilder.create((p_66890_) -> {
         return p_66890_.group(Codec.INT.fieldOf("centerX").orElse(0).forGetter((p_160382_) -> {
            return p_160382_.centerX;
         }), Codec.INT.fieldOf("centerZ").orElse(0).forGetter((p_160380_) -> {
            return p_160380_.centerZ;
         }), Codec.INT.fieldOf("radius").orElse(0).forGetter((p_160378_) -> {
            return p_160378_.radius;
         }), Codec.INT.fieldOf("height").orElse(0).forGetter((p_160376_) -> {
            return p_160376_.height;
         }), Codec.BOOL.fieldOf("guarded").orElse(false).forGetter((p_160374_) -> {
            return p_160374_.guarded;
         })).apply(p_66890_, SpikeFeature.EndSpike::new);
      });
      private final int centerX;
      private final int centerZ;
      private final int radius;
      private final int height;
      private final boolean guarded;
      private final AABB topBoundingBox;

      public EndSpike(int p_66881_, int p_66882_, int p_66883_, int p_66884_, boolean p_66885_) {
         this.centerX = p_66881_;
         this.centerZ = p_66882_;
         this.radius = p_66883_;
         this.height = p_66884_;
         this.guarded = p_66885_;
         this.topBoundingBox = new AABB((double)(p_66881_ - p_66883_), (double)DimensionType.MIN_Y, (double)(p_66882_ - p_66883_), (double)(p_66881_ + p_66883_), (double)DimensionType.MAX_Y, (double)(p_66882_ + p_66883_));
      }

      public boolean isCenterWithinChunk(BlockPos pPos) {
         return SectionPos.blockToSectionCoord(pPos.getX()) == SectionPos.blockToSectionCoord(this.centerX) && SectionPos.blockToSectionCoord(pPos.getZ()) == SectionPos.blockToSectionCoord(this.centerZ);
      }

      public int getCenterX() {
         return this.centerX;
      }

      public int getCenterZ() {
         return this.centerZ;
      }

      public int getRadius() {
         return this.radius;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean isGuarded() {
         return this.guarded;
      }

      public AABB getTopBoundingBox() {
         return this.topBoundingBox;
      }
   }

   static class SpikeCacheLoader extends CacheLoader<Long, List<SpikeFeature.EndSpike>> {
      public List<SpikeFeature.EndSpike> load(Long p_66910_) {
         IntArrayList intarraylist = Util.toShuffledList(IntStream.range(0, 10), RandomSource.create(p_66910_));
         List<SpikeFeature.EndSpike> list = Lists.newArrayList();

         for(int i = 0; i < 10; ++i) {
            int j = Mth.floor(42.0D * Math.cos(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int k = Mth.floor(42.0D * Math.sin(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
            int l = intarraylist.get(i);
            int i1 = 2 + l / 3;
            int j1 = 76 + l * 3;
            boolean flag = l == 1 || l == 2;
            list.add(new SpikeFeature.EndSpike(j, k, i1, j1, flag));
         }

         return list;
      }
   }
}