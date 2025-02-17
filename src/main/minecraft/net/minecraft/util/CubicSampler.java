package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import net.optifine.Vec3M;
import net.optifine.util.ColorSamplerM;

public class CubicSampler {
   private static final int GAUSSIAN_SAMPLE_RADIUS = 2;
   private static final int GAUSSIAN_SAMPLE_BREADTH = 6;
   private static final double[] GAUSSIAN_SAMPLE_KERNEL = new double[]{0.0D, 1.0D, 4.0D, 6.0D, 4.0D, 1.0D, 0.0D};

   private CubicSampler() {
   }

   public static Vec3 gaussianSampleVec3(Vec3 pVec, CubicSampler.Vec3Fetcher pFetcher) {
      int i = Mth.floor(pVec.x());
      int j = Mth.floor(pVec.y());
      int k = Mth.floor(pVec.z());
      double d0 = pVec.x() - (double)i;
      double d1 = pVec.y() - (double)j;
      double d2 = pVec.z() - (double)k;
      double d3 = 0.0D;
      Vec3 vec3 = Vec3.ZERO;

      for(int l = 0; l < 6; ++l) {
         double d4 = Mth.lerp(d0, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
         int i1 = i - 2 + l;

         for(int j1 = 0; j1 < 6; ++j1) {
            double d5 = Mth.lerp(d1, GAUSSIAN_SAMPLE_KERNEL[j1 + 1], GAUSSIAN_SAMPLE_KERNEL[j1]);
            int k1 = j - 2 + j1;

            for(int l1 = 0; l1 < 6; ++l1) {
               double d6 = Mth.lerp(d2, GAUSSIAN_SAMPLE_KERNEL[l1 + 1], GAUSSIAN_SAMPLE_KERNEL[l1]);
               int i2 = k - 2 + l1;
               double d7 = d4 * d5 * d6;
               d3 += d7;
               vec3 = vec3.add(pFetcher.fetch(i1, k1, i2).scale(d7));
            }
         }
      }

      return vec3.scale(1.0D / d3);
   }

   public static Vec3 sampleM(Vec3 vectorIn, ColorSamplerM fetcherIn) {
      int i = Mth.floor(vectorIn.x());
      int j = Mth.floor(vectorIn.y());
      int k = Mth.floor(vectorIn.z());
      double d0 = vectorIn.x() - (double)i;
      double d1 = vectorIn.y() - (double)j;
      double d2 = vectorIn.z() - (double)k;
      double d3 = 0.0D;
      Vec3M vec3m = new Vec3M(0.0D, 0.0D, 0.0D);

      for(int l = 0; l < 6; ++l) {
         double d4 = Mth.lerp(d0, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
         int i1 = i - 2 + l;

         for(int j1 = 0; j1 < 6; ++j1) {
            double d5 = Mth.lerp(d1, GAUSSIAN_SAMPLE_KERNEL[j1 + 1], GAUSSIAN_SAMPLE_KERNEL[j1]);
            int k1 = j - 2 + j1;

            for(int l1 = 0; l1 < 6; ++l1) {
               double d6 = Mth.lerp(d2, GAUSSIAN_SAMPLE_KERNEL[l1 + 1], GAUSSIAN_SAMPLE_KERNEL[l1]);
               int i2 = k - 2 + l1;
               double d7 = d4 * d5 * d6;
               d3 += d7;
               Vec3M vec3m1 = fetcherIn.getColor(i1, k1, i2);
               vec3m1.scale(d7);
               vec3m = vec3m.add(vec3m1);
            }
         }
      }

      return vec3m.scale(1.0D / d3).toVec3();
   }

   @FunctionalInterface
   public interface Vec3Fetcher {
      Vec3 fetch(int pX, int pY, int pZ);
   }
}