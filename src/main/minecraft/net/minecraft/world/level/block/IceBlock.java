package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class IceBlock extends HalfTransparentBlock {
   public static final MapCodec<IceBlock> CODEC = simpleCodec(IceBlock::new);

   public MapCodec<? extends IceBlock> codec() {
      return CODEC;
   }

   public IceBlock(BlockBehaviour.Properties p_54155_) {
      super(p_54155_);
   }

   public static BlockState meltsInto() {
      return Blocks.WATER.defaultBlockState();
   }

   public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pTe, ItemStack pStack) {
      super.playerDestroy(pLevel, pPlayer, pPos, pState, pTe, pStack);
      if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, pStack) == 0) {
         if (pLevel.dimensionType().ultraWarm()) {
            pLevel.removeBlock(pPos, false);
            return;
         }

         BlockState blockstate = pLevel.getBlockState(pPos.below());
         if (blockstate.blocksMotion() || blockstate.liquid()) {
            pLevel.setBlockAndUpdate(pPos, meltsInto());
         }
      }

   }

   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.getBrightness(LightLayer.BLOCK, pPos) > 11 - pState.getLightBlock(pLevel, pPos)) {
         this.melt(pState, pLevel, pPos);
      }

   }

   protected void melt(BlockState pState, Level pLevel, BlockPos pPos) {
      if (pLevel.dimensionType().ultraWarm()) {
         pLevel.removeBlock(pPos, false);
      } else {
         pLevel.setBlockAndUpdate(pPos, meltsInto());
         pLevel.neighborChanged(pPos, meltsInto().getBlock(), pPos);
      }
   }
}