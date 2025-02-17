package net.minecraft.world.item;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public class ShearsItem extends Item {
   public ShearsItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
      if (!pLevel.isClientSide && !pState.is(BlockTags.FIRE)) {
         pStack.hurtAndBreak(1, pEntityLiving, (p_43076_) -> {
            p_43076_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
         });
      }

      return !pState.is(BlockTags.LEAVES) && !pState.is(Blocks.COBWEB) && !pState.is(Blocks.SHORT_GRASS) && !pState.is(Blocks.FERN) && !pState.is(Blocks.DEAD_BUSH) && !pState.is(Blocks.HANGING_ROOTS) && !pState.is(Blocks.VINE) && !pState.is(Blocks.TRIPWIRE) && !pState.is(BlockTags.WOOL) ? super.mineBlock(pStack, pLevel, pState, pPos, pEntityLiving) : true;
   }

   public boolean isCorrectToolForDrops(BlockState pBlock) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return (pBlock.is(Blocks.COBWEB));
      }
      return pBlock.is(Blocks.COBWEB) || pBlock.is(Blocks.REDSTONE_WIRE) || pBlock.is(Blocks.TRIPWIRE);
   }

   public float getDestroySpeed(ItemStack pStack, BlockState pState) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
         if (!pState.is(Blocks.COBWEB) && !pState.is(BlockTags.LEAVES)) {
            return (pState.is(BlockTags.WOOL) ? 5.0F : super.getDestroySpeed(pStack, pState));
         } else {
            return (15.0F);
         }
      }
      if (!pState.is(Blocks.COBWEB) && !pState.is(BlockTags.LEAVES)) {
         if (pState.is(BlockTags.WOOL)) {
            return 5.0F;
         } else {
            return !pState.is(Blocks.VINE) && !pState.is(Blocks.GLOW_LICHEN) ? super.getDestroySpeed(pStack, pState) : 2.0F;
         }
      } else {
         return 15.0F;
      }
   }

   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      Block block = blockstate.getBlock();
      if (block instanceof GrowingPlantHeadBlock growingplantheadblock) {
         if (!growingplantheadblock.isMaxAge(blockstate)) {
            Player player = pContext.getPlayer();
            ItemStack itemstack = pContext.getItemInHand();
            if (player instanceof ServerPlayer) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
            }

            level.playSound(player, blockpos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
            BlockState blockstate1 = growingplantheadblock.getMaxAgeState(blockstate);
            level.setBlockAndUpdate(blockpos, blockstate1);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(pContext.getPlayer(), blockstate1));
            if (player != null) {
               itemstack.hurtAndBreak(1, player, (p_186374_) -> {
                  p_186374_.broadcastBreakEvent(pContext.getHand());
               });
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      }

      return super.useOn(pContext);
   }
}