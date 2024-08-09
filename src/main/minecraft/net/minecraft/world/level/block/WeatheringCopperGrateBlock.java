package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperGrateBlock extends WaterloggedTransparentBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperGrateBlock> CODEC = RecordCodecBuilder.mapCodec((p_313130_) -> {
      return p_313130_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperGrateBlock::getAge), propertiesCodec()).apply(p_313130_, WeatheringCopperGrateBlock::new);
   });
   private final WeatheringCopper.WeatherState weatherState;

   protected MapCodec<WeatheringCopperGrateBlock> codec() {
      return CODEC;
   }

   protected WeatheringCopperGrateBlock(WeatheringCopper.WeatherState p_311827_, BlockBehaviour.Properties p_311858_) {
      super(p_311858_);
      this.weatherState = p_311827_;
   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.changeOverTime(pState, pLevel, pPos, pRandom);
   }

   public boolean isRandomlyTicking(BlockState pState) {
      return WeatheringCopper.getNext(pState.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}