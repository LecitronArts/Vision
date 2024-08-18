package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HoeItem extends DiggerItem {
   protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.newHashMap(ImmutableMap.of(Blocks.GRASS_BLOCK, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.DIRT_PATH, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.DIRT, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.COARSE_DIRT, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.DIRT.defaultBlockState())), Blocks.ROOTED_DIRT, Pair.of((p_238242_) -> {
      return true;
   }, changeIntoStateAndDropItem(Blocks.DIRT.defaultBlockState(), Items.HANGING_ROOTS))));

   protected HoeItem(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Item.Properties pProperties) {
      super((float)pAttackDamageModifier, pAttackSpeedModifier, pTier, BlockTags.MINEABLE_WITH_HOE, pProperties);
   }
   private static final Set<Block> viaFabricPlus$effective_blocks_r1_16_5 = ImmutableSet.of(
           Blocks.NETHER_WART_BLOCK,
           Blocks.WARPED_WART_BLOCK,
           Blocks.HAY_BLOCK,
           Blocks.DRIED_KELP_BLOCK,
           Blocks.TARGET,
           Blocks.SHROOMLIGHT,
           Blocks.SPONGE,
           Blocks.WET_SPONGE,
           Blocks.JUNGLE_LEAVES,
           Blocks.OAK_LEAVES,
           Blocks.SPRUCE_LEAVES,
           Blocks.DARK_OAK_LEAVES,
           Blocks.ACACIA_LEAVES,
           Blocks.BIRCH_LEAVES
   );
   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = TILLABLES.get(level.getBlockState(blockpos).getBlock());
      if (pair == null) {
         return InteractionResult.PASS;
      } else {
         Predicate<UseOnContext> predicate = pair.getFirst();
         Consumer<UseOnContext> consumer = pair.getSecond();
         if (predicate.test(pContext)) {
            Player player = pContext.getPlayer();
            level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide) {
               consumer.accept(pContext);
               if (player != null) {
                  pContext.getItemInHand().hurtAndBreak(1, player, (p_150845_) -> {
                     p_150845_.broadcastBreakEvent(pContext.getHand());
                  });
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static Consumer<UseOnContext> changeIntoState(BlockState pState) {
      return (p_238241_) -> {
         p_238241_.getLevel().setBlock(p_238241_.getClickedPos(), pState, 11);
         p_238241_.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, p_238241_.getClickedPos(), GameEvent.Context.of(p_238241_.getPlayer(), pState));
      };
   }

   public static Consumer<UseOnContext> changeIntoStateAndDropItem(BlockState pState, ItemLike pItemToDrop) {
      return (p_238246_) -> {
         p_238246_.getLevel().setBlock(p_238246_.getClickedPos(), pState, 11);
         p_238246_.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, p_238246_.getClickedPos(), GameEvent.Context.of(p_238246_.getPlayer(), pState));
         Block.popResourceFromFace(p_238246_.getLevel(), p_238246_.getClickedPos(), p_238246_.getClickedFace(), new ItemStack(pItemToDrop));
      };
   }

   public static boolean onlyIfAirAbove(UseOnContext p_150857_) {
      return p_150857_.getClickedFace() != Direction.DOWN && p_150857_.getLevel().getBlockState(p_150857_.getClickedPos().above()).isAir();
   }

   @Override
   public boolean isCorrectToolForDrops(BlockState state) {
      return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_16_4) && super.isCorrectToolForDrops(state);
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
         return 1.0F;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
         return viaFabricPlus$effective_blocks_r1_16_5.contains(state.getBlock()) ? this.speed : 1.0F;
      } else {
         return super.getDestroySpeed(stack, state);
      }
   }
}