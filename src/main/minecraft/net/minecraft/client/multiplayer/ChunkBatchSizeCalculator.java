package net.minecraft.client.multiplayer;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChunkBatchSizeCalculator {
   private static final int MAX_OLD_SAMPLES_WEIGHT = 49;
   private static final int CLAMP_COEFFICIENT = 3;
   private double aggregatedNanosPerChunk = 2000000.0D;
   private int oldSamplesWeight = 1;
   private volatile long chunkBatchStartTime = Util.getNanos();

   public void onBatchStart() {
      this.chunkBatchStartTime = Util.getNanos();
   }

   public void onBatchFinished(int pBatchSize) {
      if (pBatchSize > 0) {
         double d0 = (double)(Util.getNanos() - this.chunkBatchStartTime);
         double d1 = d0 / (double)pBatchSize;
         double d2 = Mth.clamp(d1, this.aggregatedNanosPerChunk / 3.0D, this.aggregatedNanosPerChunk * 3.0D);
         this.aggregatedNanosPerChunk = (this.aggregatedNanosPerChunk * (double)this.oldSamplesWeight + d2) / (double)(this.oldSamplesWeight + 1);
         this.oldSamplesWeight = Math.min(49, this.oldSamplesWeight + 1);
      }

   }

   public float getDesiredChunksPerTick() {
      return (float)(7000000.0D / this.aggregatedNanosPerChunk);
   }
}