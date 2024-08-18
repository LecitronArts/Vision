package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.data.Material1_19_4;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Unique;

public class AxeItem extends DiggerItem {
   protected static final Map<Block, Block> STRIPPABLES = (new ImmutableMap.Builder<Block, Block>()).put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD).put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG).put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD).put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG).put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD).put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG).put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD).put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG).put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD).put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG).put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD).put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG).put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD).put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG).put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM).put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE).put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM).put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE).put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD).put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG).put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK).build();
   @Unique
   private static final Set<Block> viaFabricPlus$effective_blocks_b1_8_1 = ImmutableSet.of(Blocks.OAK_PLANKS, Blocks.BOOKSHELF, Blocks.OAK_LOG, Blocks.CHEST);

   @Unique
   private static final Set<Block> viaFabricPlus$effective_blocks_r1_16_5 = ImmutableSet.of(Blocks.LADDER, Blocks.SCAFFOLDING, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON);

   @Unique
   private static final Set<Material1_19_4> viaFabricPlus$effective_materials_r1_16_5 = ImmutableSet.of(Material1_19_4.WOOD, Material1_19_4.NETHER_WOOD, Material1_19_4.PLANT, Material1_19_4.REPLACEABLE_PLANT, Material1_19_4.BAMBOO, Material1_19_4.GOURD);

   protected AxeItem(Tier pTier, float pAttackDamageModifier, float pAttackSpeedModifier, Item.Properties pProperties) {
      super(pAttackDamageModifier, pAttackSpeedModifier, pTier, BlockTags.MINEABLE_WITH_AXE, pProperties);
   }
   @Override
   public boolean isCorrectToolForDrops(BlockState state) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
         return false;
      } else {
         return super.isCorrectToolForDrops(state);
      }
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return viaFabricPlus$effective_blocks_b1_8_1.contains(state.getBlock()) ? this.speed : 1.0F;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
         return viaFabricPlus$effective_materials_r1_16_5.contains(Material1_19_4.getMaterial(state)) ? this.speed : viaFabricPlus$effective_blocks_r1_16_5.contains(state.getBlock()) ? this.speed : 1.0F;
      } else {
         return super.getDestroySpeed(stack, state);
      }
   }

   public InteractionResult useOn(UseOnContext pContext) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return  (InteractionResult.PASS);
      }
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      Player player = pContext.getPlayer();
      Optional<BlockState> optional = this.evaluateNewBlockState(level, blockpos, player, level.getBlockState(blockpos));
      if (optional.isEmpty()) {
         return InteractionResult.PASS;
      } else {
         ItemStack itemstack = pContext.getItemInHand();
         if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
         }

         level.setBlock(blockpos, optional.get(), 11);
         level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, optional.get()));
         if (player != null) {
            itemstack.hurtAndBreak(1, player, (p_150686_) -> {
               p_150686_.broadcastBreakEvent(pContext.getHand());
            });
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   private Optional<BlockState> evaluateNewBlockState(Level pLevel, BlockPos pPos, @Nullable Player pPlayer, BlockState pState) {
      Optional<BlockState> optional = this.getStripped(pState);
      if (optional.isPresent()) {
         pLevel.playSound(pPlayer, pPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
         return optional;
      } else {
         Optional<BlockState> optional1 = WeatheringCopper.getPrevious(pState);
         if (optional1.isPresent()) {
            pLevel.playSound(pPlayer, pPos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            pLevel.levelEvent(pPlayer, 3005, pPos, 0);
            return optional1;
         } else {
            Optional<BlockState> optional2 = Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(pState.getBlock())).map((p_150694_) -> {
               return p_150694_.withPropertiesOf(pState);
            });
            if (optional2.isPresent()) {
               pLevel.playSound(pPlayer, pPos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
               pLevel.levelEvent(pPlayer, 3004, pPos, 0);
               return optional2;
            } else {
               return Optional.empty();
            }
         }
      }
   }

   private Optional<BlockState> getStripped(BlockState pUnstrippedState) {
      return Optional.ofNullable(STRIPPABLES.get(pUnstrippedState.getBlock())).map((p_150689_) -> {
         return p_150689_.defaultBlockState().setValue(RotatedPillarBlock.AXIS, pUnstrippedState.getValue(RotatedPillarBlock.AXIS));
      });
   }
}