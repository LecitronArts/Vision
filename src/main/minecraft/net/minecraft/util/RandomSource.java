package net.minecraft.util;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;

public interface RandomSource {
   /** @deprecated */
   @Deprecated
   double GAUSSIAN_SPREAD_FACTOR = 2.297D;

   static RandomSource create() {
      return create(RandomSupport.generateUniqueSeed());
   }

   /** @deprecated */
   @Deprecated
   static RandomSource createThreadSafe() {
      return new ThreadSafeLegacyRandomSource(RandomSupport.generateUniqueSeed());
   }

   static RandomSource create(long pSeed) {
      return new LegacyRandomSource(pSeed);
   }

   static RandomSource createNewThreadLocalInstance() {
      return new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
   }

   RandomSource fork();

   PositionalRandomFactory forkPositional();

   void setSeed(long pSeed);

   int nextInt();

   int nextInt(int pBound);

   default int nextIntBetweenInclusive(int pMin, int pMax) {
      return this.nextInt(pMax - pMin + 1) + pMin;
   }

   long nextLong();

   boolean nextBoolean();

   float nextFloat();

   double nextDouble();

   double nextGaussian();

   default double triangle(double pMin, double pMax) {
      return pMin + pMax * (this.nextDouble() - this.nextDouble());
   }

   default void consumeCount(int pCount) {
      for(int i = 0; i < pCount; ++i) {
         this.nextInt();
      }

   }

   default int nextInt(int pOrigin, int pBound) {
      if (pOrigin >= pBound) {
         throw new IllegalArgumentException("bound - origin is non positive");
      } else {
         return pOrigin + this.nextInt(pBound - pOrigin);
      }
   }
}