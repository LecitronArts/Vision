package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperBulbBlock extends CopperBulbBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperBulbBlock> CODEC = RecordCodecBuilder.mapCodec((p_311316_) -> {
      return p_311316_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperBulbBlock::getAge), propertiesCodec()).apply(p_311316_, WeatheringCopperBulbBlock::new);
   });
   private final WeatheringCopper.WeatherState weatherState;

   protected MapCodec<WeatheringCopperBulbBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperBulbBlock(WeatheringCopper.WeatherState p_309695_, BlockBehaviour.Properties p_311798_) {
      super(p_311798_);
      this.weatherState = p_309695_;
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