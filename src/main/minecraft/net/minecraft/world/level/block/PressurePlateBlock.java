package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PressurePlateBlock extends BasePressurePlateBlock {
   public static final MapCodec<PressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec((p_310452_) -> {
      return p_310452_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter((p_313030_) -> {
         return p_313030_.type;
      }), propertiesCodec()).apply(p_310452_, PressurePlateBlock::new);
   });
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public MapCodec<PressurePlateBlock> codec() {
      return CODEC;
   }

   protected PressurePlateBlock(BlockSetType p_273284_, BlockBehaviour.Properties p_273571_) {
      super(p_273571_, p_273284_);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.valueOf(false)));
   }

   protected int getSignalForState(BlockState pState) {
      return pState.getValue(POWERED) ? 15 : 0;
   }

   protected BlockState setSignalForState(BlockState pState, int pStrength) {
      return pState.setValue(POWERED, Boolean.valueOf(pStrength > 0));
   }

   protected int getSignalStrength(Level pLevel, BlockPos pPos) {
      Class<Entity> oclass1;
      switch (this.type.pressurePlateSensitivity()) {
         case EVERYTHING:
            oclass1 = Entity.class;
            break;
         case MOBS:
            oclass1 = (Class)LivingEntity.class;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      Class<? extends Entity> oclass = oclass1;
      return getEntityCount(pLevel, TOUCH_AABB.move(pPos), oclass) > 0 ? 15 : 0;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(POWERED);
   }
}