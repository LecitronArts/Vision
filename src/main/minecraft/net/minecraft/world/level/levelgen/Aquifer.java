package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
   static Aquifer create(NoiseChunk pChunk, ChunkPos pChunkPos, NoiseRouter pNoiseRouter, PositionalRandomFactory pPositionalRandomFactory, int pMinY, int pHeight, Aquifer.FluidPicker pGlobalFluidPicker) {
      return new Aquifer.NoiseBasedAquifer(pChunk, pChunkPos, pNoiseRouter, pPositionalRandomFactory, pMinY, pHeight, pGlobalFluidPicker);
   }

   static Aquifer createDisabled(final Aquifer.FluidPicker pDefaultFluid) {
      return new Aquifer() {
         @Nullable
         public BlockState computeSubstance(DensityFunction.FunctionContext p_208172_, double p_208173_) {
            return p_208173_ > 0.0D ? null : pDefaultFluid.computeFluid(p_208172_.blockX(), p_208172_.blockY(), p_208172_.blockZ()).at(p_208172_.blockY());
         }

         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(DensityFunction.FunctionContext pContext, double pSubstance);

   boolean shouldScheduleFluidUpdate();

   public interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ);
   }

   public static final class FluidStatus {
      final int fluidLevel;
      final BlockState fluidType;

      public FluidStatus(int pFluidLevel, BlockState pFluidType) {
         this.fluidLevel = pFluidLevel;
         this.fluidType = pFluidType;
      }

      public BlockState at(int pY) {
         return pY < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   public static class NoiseBasedAquifer implements Aquifer {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private final NoiseChunk noiseChunk;
      private final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      private final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      private final Aquifer.FluidStatus[] aquiferCache;
      private final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      private boolean shouldScheduleFluidUpdate;
      private final int minGridX;
      private final int minGridY;
      private final int minGridZ;
      private final int gridSizeX;
      private final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{{0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}};

      NoiseBasedAquifer(NoiseChunk pNoiseChunk, ChunkPos pChunkPos, NoiseRouter pNoiseRouter, PositionalRandomFactory pPositionalRandomFactory, int pMinY, int pHeight, Aquifer.FluidPicker pGlobalFluidPicker) {
         this.noiseChunk = pNoiseChunk;
         this.barrierNoise = pNoiseRouter.barrierNoise();
         this.fluidLevelFloodednessNoise = pNoiseRouter.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = pNoiseRouter.fluidLevelSpreadNoise();
         this.lavaNoise = pNoiseRouter.lavaNoise();
         this.erosion = pNoiseRouter.erosion();
         this.depth = pNoiseRouter.depth();
         this.positionalRandomFactory = pPositionalRandomFactory;
         this.minGridX = this.gridX(pChunkPos.getMinBlockX()) - 1;
         this.globalFluidPicker = pGlobalFluidPicker;
         int i = this.gridX(pChunkPos.getMaxBlockX()) + 1;
         this.gridSizeX = i - this.minGridX + 1;
         this.minGridY = this.gridY(pMinY) - 1;
         int j = this.gridY(pMinY + pHeight) + 1;
         int k = j - this.minGridY + 1;
         this.minGridZ = this.gridZ(pChunkPos.getMinBlockZ()) - 1;
         int l = this.gridZ(pChunkPos.getMaxBlockZ()) + 1;
         this.gridSizeZ = l - this.minGridZ + 1;
         int i1 = this.gridSizeX * k * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[i1];
         this.aquiferLocationCache = new long[i1];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
      }

      private int getIndex(int pGridX, int pGridY, int pGridZ) {
         int i = pGridX - this.minGridX;
         int j = pGridY - this.minGridY;
         int k = pGridZ - this.minGridZ;
         return (j * this.gridSizeZ + k) * this.gridSizeX + i;
      }

      @Nullable
      public BlockState computeSubstance(DensityFunction.FunctionContext pContext, double pSubstance) {
         int i = pContext.blockX();
         int j = pContext.blockY();
         int k = pContext.blockZ();
         if (pSubstance > 0.0D) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         } else {
            Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(i, j, k);
            if (aquifer$fluidstatus.at(j).is(Blocks.LAVA)) {
               this.shouldScheduleFluidUpdate = false;
               return Blocks.LAVA.defaultBlockState();
            } else {
               int l = Math.floorDiv(i - 5, 16);
               int i1 = Math.floorDiv(j + 1, 12);
               int j1 = Math.floorDiv(k - 5, 16);
               int k1 = Integer.MAX_VALUE;
               int l1 = Integer.MAX_VALUE;
               int i2 = Integer.MAX_VALUE;
               long j2 = 0L;
               long k2 = 0L;
               long l2 = 0L;

               for(int i3 = 0; i3 <= 1; ++i3) {
                  for(int j3 = -1; j3 <= 1; ++j3) {
                     for(int k3 = 0; k3 <= 1; ++k3) {
                        int l3 = l + i3;
                        int i4 = i1 + j3;
                        int j4 = j1 + k3;
                        int k4 = this.getIndex(l3, i4, j4);
                        long i5 = this.aquiferLocationCache[k4];
                        long l4;
                        if (i5 != Long.MAX_VALUE) {
                           l4 = i5;
                        } else {
                           RandomSource randomsource = this.positionalRandomFactory.at(l3, i4, j4);
                           l4 = BlockPos.asLong(l3 * 16 + randomsource.nextInt(10), i4 * 12 + randomsource.nextInt(9), j4 * 16 + randomsource.nextInt(10));
                           this.aquiferLocationCache[k4] = l4;
                        }

                        int i6 = BlockPos.getX(l4) - i;
                        int j5 = BlockPos.getY(l4) - j;
                        int k5 = BlockPos.getZ(l4) - k;
                        int l5 = i6 * i6 + j5 * j5 + k5 * k5;
                        if (k1 >= l5) {
                           l2 = k2;
                           k2 = j2;
                           j2 = l4;
                           i2 = l1;
                           l1 = k1;
                           k1 = l5;
                        } else if (l1 >= l5) {
                           l2 = k2;
                           k2 = l4;
                           i2 = l1;
                           l1 = l5;
                        } else if (i2 >= l5) {
                           l2 = l4;
                           i2 = l5;
                        }
                     }
                  }
               }

               Aquifer.FluidStatus aquifer$fluidstatus1 = this.getAquiferStatus(j2);
               double d1 = similarity(k1, l1);
               BlockState blockstate = aquifer$fluidstatus1.at(j);
               if (d1 <= 0.0D) {
                  this.shouldScheduleFluidUpdate = d1 >= FLOWING_UPDATE_SIMULARITY;
                  return blockstate;
               } else if (blockstate.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(i, j - 1, k).at(j - 1).is(Blocks.LAVA)) {
                  this.shouldScheduleFluidUpdate = true;
                  return blockstate;
               } else {
                  MutableDouble mutabledouble = new MutableDouble(Double.NaN);
                  Aquifer.FluidStatus aquifer$fluidstatus2 = this.getAquiferStatus(k2);
                  double d2 = d1 * this.calculatePressure(pContext, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus2);
                  if (pSubstance + d2 > 0.0D) {
                     this.shouldScheduleFluidUpdate = false;
                     return null;
                  } else {
                     Aquifer.FluidStatus aquifer$fluidstatus3 = this.getAquiferStatus(l2);
                     double d0 = similarity(k1, i2);
                     if (d0 > 0.0D) {
                        double d3 = d1 * d0 * this.calculatePressure(pContext, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus3);
                        if (pSubstance + d3 > 0.0D) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     double d4 = similarity(l1, i2);
                     if (d4 > 0.0D) {
                        double d5 = d1 * d4 * this.calculatePressure(pContext, mutabledouble, aquifer$fluidstatus2, aquifer$fluidstatus3);
                        if (pSubstance + d5 > 0.0D) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     this.shouldScheduleFluidUpdate = true;
                     return blockstate;
                  }
               }
            }
         }
      }

      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      private static double similarity(int pFirstDistance, int pSecondDistance) {
         double d0 = 25.0D;
         return 1.0D - (double)Math.abs(pSecondDistance - pFirstDistance) / 25.0D;
      }

      private double calculatePressure(DensityFunction.FunctionContext pContext, MutableDouble pSubstance, Aquifer.FluidStatus pFirstFluid, Aquifer.FluidStatus pSecondFluid) {
         int i = pContext.blockY();
         BlockState blockstate = pFirstFluid.at(i);
         BlockState blockstate1 = pSecondFluid.at(i);
         if ((!blockstate.is(Blocks.LAVA) || !blockstate1.is(Blocks.WATER)) && (!blockstate.is(Blocks.WATER) || !blockstate1.is(Blocks.LAVA))) {
            int j = Math.abs(pFirstFluid.fluidLevel - pSecondFluid.fluidLevel);
            if (j == 0) {
               return 0.0D;
            } else {
               double d0 = 0.5D * (double)(pFirstFluid.fluidLevel + pSecondFluid.fluidLevel);
               double d1 = (double)i + 0.5D - d0;
               double d2 = (double)j / 2.0D;
               double d3 = 0.0D;
               double d4 = 2.5D;
               double d5 = 1.5D;
               double d6 = 3.0D;
               double d7 = 10.0D;
               double d8 = 3.0D;
               double d9 = d2 - Math.abs(d1);
               double d10;
               if (d1 > 0.0D) {
                  double d11 = 0.0D + d9;
                  if (d11 > 0.0D) {
                     d10 = d11 / 1.5D;
                  } else {
                     d10 = d11 / 2.5D;
                  }
               } else {
                  double d15 = 3.0D + d9;
                  if (d15 > 0.0D) {
                     d10 = d15 / 3.0D;
                  } else {
                     d10 = d15 / 10.0D;
                  }
               }

               double d16 = 2.0D;
               double d12;
               if (!(d10 < -2.0D) && !(d10 > 2.0D)) {
                  double d13 = pSubstance.getValue();
                  if (Double.isNaN(d13)) {
                     double d14 = this.barrierNoise.compute(pContext);
                     pSubstance.setValue(d14);
                     d12 = d14;
                  } else {
                     d12 = d13;
                  }
               } else {
                  d12 = 0.0D;
               }

               return 2.0D * (d12 + d10);
            }
         } else {
            return 2.0D;
         }
      }

      private int gridX(int pX) {
         return Math.floorDiv(pX, 16);
      }

      private int gridY(int pY) {
         return Math.floorDiv(pY, 12);
      }

      private int gridZ(int pZ) {
         return Math.floorDiv(pZ, 16);
      }

      private Aquifer.FluidStatus getAquiferStatus(long pPackedPos) {
         int i = BlockPos.getX(pPackedPos);
         int j = BlockPos.getY(pPackedPos);
         int k = BlockPos.getZ(pPackedPos);
         int l = this.gridX(i);
         int i1 = this.gridY(j);
         int j1 = this.gridZ(k);
         int k1 = this.getIndex(l, i1, j1);
         Aquifer.FluidStatus aquifer$fluidstatus = this.aquiferCache[k1];
         if (aquifer$fluidstatus != null) {
            return aquifer$fluidstatus;
         } else {
            Aquifer.FluidStatus aquifer$fluidstatus1 = this.computeFluid(i, j, k);
            this.aquiferCache[k1] = aquifer$fluidstatus1;
            return aquifer$fluidstatus1;
         }
      }

      private Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ) {
         Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(pX, pY, pZ);
         int i = Integer.MAX_VALUE;
         int j = pY + 12;
         int k = pY - 12;
         boolean flag = false;

         for(int[] aint : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int l = pX + SectionPos.sectionToBlockCoord(aint[0]);
            int i1 = pZ + SectionPos.sectionToBlockCoord(aint[1]);
            int j1 = this.noiseChunk.preliminarySurfaceLevel(l, i1);
            int k1 = j1 + 8;
            boolean flag1 = aint[0] == 0 && aint[1] == 0;
            if (flag1 && k > k1) {
               return aquifer$fluidstatus;
            }

            boolean flag2 = j > k1;
            if (flag2 || flag1) {
               Aquifer.FluidStatus aquifer$fluidstatus1 = this.globalFluidPicker.computeFluid(l, k1, i1);
               if (!aquifer$fluidstatus1.at(k1).isAir()) {
                  if (flag1) {
                     flag = true;
                  }

                  if (flag2) {
                     return aquifer$fluidstatus1;
                  }
               }
            }

            i = Math.min(i, j1);
         }

         int l1 = this.computeSurfaceLevel(pX, pY, pZ, aquifer$fluidstatus, i, flag);
         return new Aquifer.FluidStatus(l1, this.computeFluidType(pX, pY, pZ, aquifer$fluidstatus, l1));
      }

      private int computeSurfaceLevel(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pMaxSurfaceLevel, boolean pFluidPresent) {
         DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(pX, pY, pZ);
         double d0;
         double d1;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, densityfunction$singlepointcontext)) {
            d0 = -1.0D;
            d1 = -1.0D;
         } else {
            int i = pMaxSurfaceLevel + 8 - pY;
            int j = 64;
            double d2 = pFluidPresent ? Mth.clampedMap((double)i, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
            double d3 = Mth.clamp(this.fluidLevelFloodednessNoise.compute(densityfunction$singlepointcontext), -1.0D, 1.0D);
            double d4 = Mth.map(d2, 1.0D, 0.0D, -0.3D, 0.8D);
            double d5 = Mth.map(d2, 1.0D, 0.0D, -0.8D, 0.4D);
            d0 = d3 - d5;
            d1 = d3 - d4;
         }

         int k;
         if (d1 > 0.0D) {
            k = pFluidStatus.fluidLevel;
         } else if (d0 > 0.0D) {
            k = this.computeRandomizedFluidSurfaceLevel(pX, pY, pZ, pMaxSurfaceLevel);
         } else {
            k = DimensionType.WAY_BELOW_MIN_Y;
         }

         return k;
      }

      private int computeRandomizedFluidSurfaceLevel(int pX, int pY, int pZ, int pMaxSurfaceLevel) {
         int i = 16;
         int j = 40;
         int k = Math.floorDiv(pX, 16);
         int l = Math.floorDiv(pY, 40);
         int i1 = Math.floorDiv(pZ, 16);
         int j1 = l * 40 + 20;
         int k1 = 10;
         double d0 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(k, l, i1)) * 10.0D;
         int l1 = Mth.quantize(d0, 3);
         int i2 = j1 + l1;
         return Math.min(pMaxSurfaceLevel, i2);
      }

      private BlockState computeFluidType(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pSurfaceLevel) {
         BlockState blockstate = pFluidStatus.fluidType;
         if (pSurfaceLevel <= -10 && pSurfaceLevel != DimensionType.WAY_BELOW_MIN_Y && pFluidStatus.fluidType != Blocks.LAVA.defaultBlockState()) {
            int i = 64;
            int j = 40;
            int k = Math.floorDiv(pX, 64);
            int l = Math.floorDiv(pY, 40);
            int i1 = Math.floorDiv(pZ, 64);
            double d0 = this.lavaNoise.compute(new DensityFunction.SinglePointContext(k, l, i1));
            if (Math.abs(d0) > 0.3D) {
               blockstate = Blocks.LAVA.defaultBlockState();
            }
         }

         return blockstate;
      }
   }
}