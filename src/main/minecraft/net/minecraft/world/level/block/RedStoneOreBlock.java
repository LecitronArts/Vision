package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RedStoneOreBlock extends Block {
   public static final MapCodec<RedStoneOreBlock> CODEC = simpleCodec(RedStoneOreBlock::new);
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   public MapCodec<RedStoneOreBlock> codec() {
      return CODEC;
   }

   public RedStoneOreBlock(BlockBehaviour.Properties p_55453_) {
      super(p_55453_);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
   }

   public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      interact(pState, pLevel, pPos);
      super.attack(pState, pLevel, pPos, pPlayer);
   }

   public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity) {
      if (!pEntity.isSteppingCarefully()) {
         interact(pState, pLevel, pPos);
      }

      super.stepOn(pLevel, pPos, pState, pEntity);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pLevel.isClientSide) {
         spawnParticles(pLevel, pPos);
      } else {
         interact(pState, pLevel, pPos);
      }

      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      return itemstack.getItem() instanceof BlockItem && (new BlockPlaceContext(pPlayer, pHand, itemstack, pHit)).canPlace() ? InteractionResult.PASS : InteractionResult.SUCCESS;
   }

   private static void interact(BlockState pState, Level pLevel, BlockPos pPos) {
      spawnParticles(pLevel, pPos);
      if (!pState.getValue(LIT)) {
         pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(true)), 3);
      }

   }

   public boolean isRandomlyTicking(BlockState pState) {
      return pState.getValue(LIT);
   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(LIT)) {
         pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(false)), 3);
      }

   }

   public void spawnAfterBreak(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean pDropExperience) {
      super.spawnAfterBreak(pState, pLevel, pPos, pStack, pDropExperience);
      if (pDropExperience && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, pStack) == 0) {
         int i = 1 + pLevel.random.nextInt(5);
         this.popExperience(pLevel, pPos, i);
      }

   }

   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(LIT)) {
         spawnParticles(pLevel, pPos);
      }

   }

   private static void spawnParticles(Level pLevel, BlockPos pPos) {
      double d0 = 0.5625D;
      RandomSource randomsource = pLevel.random;

      for(Direction direction : Direction.values()) {
         BlockPos blockpos = pPos.relative(direction);
         if (!pLevel.getBlockState(blockpos).isSolidRender(pLevel, blockpos)) {
            Direction.Axis direction$axis = direction.getAxis();
            double d1 = direction$axis == Direction.Axis.X ? 0.5D + 0.5625D * (double)direction.getStepX() : (double)randomsource.nextFloat();
            double d2 = direction$axis == Direction.Axis.Y ? 0.5D + 0.5625D * (double)direction.getStepY() : (double)randomsource.nextFloat();
            double d3 = direction$axis == Direction.Axis.Z ? 0.5D + 0.5625D * (double)direction.getStepZ() : (double)randomsource.nextFloat();
            pLevel.addParticle(DustParticleOptions.REDSTONE, (double)pPos.getX() + d1, (double)pPos.getY() + d2, (double)pPos.getZ() + d3, 0.0D, 0.0D, 0.0D);
         }
      }

   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LIT);
   }
}