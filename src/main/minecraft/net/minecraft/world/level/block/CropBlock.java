package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CropBlock extends BushBlock implements BonemealableBlock {
   public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
   private static final VoxelShape viaFabricPlus$shape_r1_8_x = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);

   public MapCodec<? extends CropBlock> codec() {
      return CODEC;
   }

   protected CropBlock(BlockBehaviour.Properties p_52247_) {
      super(p_52247_);
      this.registerDefaultState(this.stateDefinition.any().setValue(this.getAgeProperty(), Integer.valueOf(0)));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return (viaFabricPlus$shape_r1_8_x);
      }
      return SHAPE_BY_AGE[this.getAge(pState)];
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.is(Blocks.FARMLAND);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 7;
   }

   public int getAge(BlockState pState) {
      return pState.getValue(this.getAgeProperty());
   }

   public BlockState getStateForAge(int pAge) {
      return this.defaultBlockState().setValue(this.getAgeProperty(), Integer.valueOf(pAge));
   }

   public final boolean isMaxAge(BlockState pState) {
      return this.getAge(pState) >= this.getMaxAge();
   }

   public boolean isRandomlyTicking(BlockState pState) {
      return !this.isMaxAge(pState);
   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.getRawBrightness(pPos, 0) >= 9) {
         int i = this.getAge(pState);
         if (i < this.getMaxAge()) {
            float f = getGrowthSpeed(this, pLevel, pPos);
            if (pRandom.nextInt((int)(25.0F / f) + 1) == 0) {
               pLevel.setBlock(pPos, this.getStateForAge(i + 1), 2);
            }
         }
      }

   }

   public void growCrops(Level pLevel, BlockPos pPos, BlockState pState) {
      int i = this.getAge(pState) + this.getBonemealAgeIncrease(pLevel);
      int j = this.getMaxAge();
      if (i > j) {
         i = j;
      }

      pLevel.setBlock(pPos, this.getStateForAge(i), 2);
   }

   protected int getBonemealAgeIncrease(Level pLevel) {
      return Mth.nextInt(pLevel.random, 2, 5);
   }

   protected static float getGrowthSpeed(Block pBlock, BlockGetter pLevel, BlockPos pPos) {
      float f = 1.0F;
      BlockPos blockpos = pPos.below();

      for(int i = -1; i <= 1; ++i) {
         for(int j = -1; j <= 1; ++j) {
            float f1 = 0.0F;
            BlockState blockstate = pLevel.getBlockState(blockpos.offset(i, 0, j));
            if (blockstate.is(Blocks.FARMLAND)) {
               f1 = 1.0F;
               if (blockstate.getValue(FarmBlock.MOISTURE) > 0) {
                  f1 = 3.0F;
               }
            }

            if (i != 0 || j != 0) {
               f1 /= 4.0F;
            }

            f += f1;
         }
      }

      BlockPos blockpos1 = pPos.north();
      BlockPos blockpos2 = pPos.south();
      BlockPos blockpos3 = pPos.west();
      BlockPos blockpos4 = pPos.east();
      boolean flag = pLevel.getBlockState(blockpos3).is(pBlock) || pLevel.getBlockState(blockpos4).is(pBlock);
      boolean flag1 = pLevel.getBlockState(blockpos1).is(pBlock) || pLevel.getBlockState(blockpos2).is(pBlock);
      if (flag && flag1) {
         f /= 2.0F;
      } else {
         boolean flag2 = pLevel.getBlockState(blockpos3.north()).is(pBlock) || pLevel.getBlockState(blockpos4.north()).is(pBlock) || pLevel.getBlockState(blockpos4.south()).is(pBlock) || pLevel.getBlockState(blockpos3.south()).is(pBlock);
         if (flag2) {
            f /= 2.0F;
         }
      }

      return f;
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return hasSufficientLight(pLevel, pPos) && super.canSurvive(pState, pLevel, pPos);
   }

   protected static boolean hasSufficientLight(LevelReader pLevel, BlockPos pPos) {
      return pLevel.getRawBrightness(pPos, 0) >= 8;
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (pEntity instanceof Ravager && pLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
         pLevel.destroyBlock(pPos, true, pEntity);
      }

      super.entityInside(pState, pLevel, pPos, pEntity);
   }

   protected ItemLike getBaseSeedId() {
      return Items.WHEAT_SEEDS;
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return new ItemStack(this.getBaseSeedId());
   }

   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return !this.isMaxAge(pState);
   }

   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      this.growCrops(pLevel, pPos, pState);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(AGE);
   }
}