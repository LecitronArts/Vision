package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DecoratedPotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<DecoratedPotBlock> CODEC = simpleCodec(DecoratedPotBlock::new);
   public static final ResourceLocation SHERDS_DYNAMIC_DROP_ID = new ResourceLocation("sherds");
   private static final VoxelShape BOUNDING_BOX = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
   private static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty CRACKED = BlockStateProperties.CRACKED;
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public MapCodec<DecoratedPotBlock> codec() {
      return CODEC;
   }

   protected DecoratedPotBlock(BlockBehaviour.Properties p_273064_) {
      super(p_273064_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(CRACKED, Boolean.valueOf(false)));
   }

   public BlockState updateShape(BlockState p_276307_, Direction p_276322_, BlockState p_276280_, LevelAccessor p_276320_, BlockPos p_276270_, BlockPos p_276312_) {
      if (p_276307_.getValue(WATERLOGGED)) {
         p_276320_.scheduleTick(p_276270_, Fluids.WATER, Fluids.WATER.getTickDelay(p_276320_));
      }

      return super.updateShape(p_276307_, p_276322_, p_276280_, p_276320_, p_276270_, p_276312_);
   }

   public BlockState getStateForPlacement(BlockPlaceContext p_272711_) {
      FluidState fluidstate = p_272711_.getLevel().getFluidState(p_272711_.getClickedPos());
      return this.defaultBlockState().setValue(HORIZONTAL_FACING, p_272711_.getHorizontalDirection()).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER)).setValue(CRACKED, Boolean.valueOf(false));
   }

   public InteractionResult use(BlockState p_312109_, Level p_312351_, BlockPos p_310007_, Player p_311727_, InteractionHand p_309947_, BlockHitResult p_311602_) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {
         return (InteractionResult.PASS);
      }
      BlockEntity $$8 = p_312351_.getBlockEntity(p_310007_);
      if ($$8 instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         if (p_312351_.isClientSide) {
            return InteractionResult.CONSUME;
         } else {
            ItemStack itemstack2 = p_311727_.getItemInHand(p_309947_);
            ItemStack itemstack = decoratedpotblockentity.getTheItem();
            if (!itemstack2.isEmpty() && (itemstack.isEmpty() || ItemStack.isSameItemSameTags(itemstack, itemstack2) && itemstack.getCount() < itemstack.getMaxStackSize())) {
               decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
               p_311727_.awardStat(Stats.ITEM_USED.get(itemstack2.getItem()));
               ItemStack itemstack1 = p_311727_.isCreative() ? itemstack2.copyWithCount(1) : itemstack2.split(1);
               float f;
               if (decoratedpotblockentity.isEmpty()) {
                  decoratedpotblockentity.setTheItem(itemstack1);
                  f = (float)itemstack1.getCount() / (float)itemstack1.getMaxStackSize();
               } else {
                  itemstack.grow(1);
                  f = (float)itemstack.getCount() / (float)itemstack.getMaxStackSize();
               }

               p_312351_.playSound((Player)null, p_310007_, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 0.7F + 0.5F * f);
               if (p_312351_ instanceof ServerLevel) {
                  ServerLevel serverlevel = (ServerLevel)p_312351_;
                  serverlevel.sendParticles(ParticleTypes.DUST_PLUME, (double)p_310007_.getX() + 0.5D, (double)p_310007_.getY() + 1.2D, (double)p_310007_.getZ() + 0.5D, 7, 0.0D, 0.0D, 0.0D, 0.0D);
               }

               decoratedpotblockentity.setChanged();
            } else {
               p_312351_.playSound((Player)null, p_310007_, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
               decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
            }

            p_312351_.gameEvent(p_311727_, GameEvent.BLOCK_CHANGE, p_310007_);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public void setPlacedBy(Level p_298316_, BlockPos p_299890_, BlockState p_299766_, @Nullable LivingEntity p_298315_, ItemStack p_299989_) {
      if (p_298316_.isClientSide) {
         p_298316_.getBlockEntity(p_299890_, BlockEntityType.DECORATED_POT).ifPresent((p_296941_) -> {
            p_296941_.setFromItem(p_299989_);
         });
      }

   }

   public boolean isPathfindable(BlockState p_276295_, BlockGetter p_276308_, BlockPos p_276313_, PathComputationType p_276303_) {
      return false;
   }

   public VoxelShape getShape(BlockState p_273112_, BlockGetter p_273055_, BlockPos p_273137_, CollisionContext p_273151_) {
      return BOUNDING_BOX;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_273169_) {
      p_273169_.add(HORIZONTAL_FACING, WATERLOGGED, CRACKED);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos p_273396_, BlockState p_272674_) {
      return new DecoratedPotBlockEntity(p_273396_, p_272674_);
   }

   public void onRemove(BlockState p_312694_, Level p_313251_, BlockPos p_312873_, BlockState p_312133_, boolean p_311809_) {
      Containers.dropContentsOnDestroy(p_312694_, p_312133_, p_313251_, p_312873_);
      super.onRemove(p_312694_, p_313251_, p_312873_, p_312133_, p_311809_);
   }

   public List<ItemStack> getDrops(BlockState p_287683_, LootParams.Builder p_287582_) {
      BlockEntity blockentity = p_287582_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         p_287582_.withDynamicDrop(SHERDS_DYNAMIC_DROP_ID, (p_284876_) -> {
            decoratedpotblockentity.getDecorations().sorted().map(Item::getDefaultInstance).forEach(p_284876_);
         });
      }

      return super.getDrops(p_287683_, p_287582_);
   }

   public BlockState playerWillDestroy(Level p_273590_, BlockPos p_273343_, BlockState p_272869_, Player p_273002_) {
      ItemStack itemstack = p_273002_.getMainHandItem();
      BlockState blockstate = p_272869_;
      if (itemstack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasSilkTouch(itemstack)) {
         blockstate = p_272869_.setValue(CRACKED, Boolean.valueOf(true));
         p_273590_.setBlock(p_273343_, blockstate, 4);
      }

      return super.playerWillDestroy(p_273590_, p_273343_, blockstate, p_273002_);
   }

   public FluidState getFluidState(BlockState p_272593_) {
      return p_272593_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_272593_);
   }

   public SoundType getSoundType(BlockState p_277561_) {
      return p_277561_.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
   }

   public void appendHoverText(ItemStack p_285238_, @Nullable BlockGetter p_285450_, List<Component> p_285448_, TooltipFlag p_284997_) {
      super.appendHoverText(p_285238_, p_285450_, p_285448_, p_284997_);
      DecoratedPotBlockEntity.Decorations decoratedpotblockentity$decorations = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(p_285238_));
      if (!decoratedpotblockentity$decorations.equals(DecoratedPotBlockEntity.Decorations.EMPTY)) {
         p_285448_.add(CommonComponents.EMPTY);
         Stream.of(decoratedpotblockentity$decorations.front(), decoratedpotblockentity$decorations.left(), decoratedpotblockentity$decorations.right(), decoratedpotblockentity$decorations.back()).forEach((p_284873_) -> {
            p_285448_.add((new ItemStack(p_284873_, 1)).getHoverName().plainCopy().withStyle(ChatFormatting.GRAY));
         });
      }
   }

   public void onProjectileHit(Level p_310477_, BlockState p_309479_, BlockHitResult p_309542_, Projectile p_309867_) {
      BlockPos blockpos = p_309542_.getBlockPos();
      if (!p_310477_.isClientSide && p_309867_.mayInteract(p_310477_, blockpos) && p_309867_.mayBreak(p_310477_)) {
         p_310477_.setBlock(blockpos, p_309479_.setValue(CRACKED, Boolean.valueOf(true)), 4);
         p_310477_.destroyBlock(blockpos, true, p_309867_);
      }

   }

   public ItemStack getCloneItemStack(LevelReader p_312375_, BlockPos p_300759_, BlockState p_297348_) {
      BlockEntity blockentity = p_312375_.getBlockEntity(p_300759_);
      if (blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         return decoratedpotblockentity.getPotAsItem();
      } else {
         return super.getCloneItemStack(p_312375_, p_300759_, p_297348_);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState p_310567_) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState p_310830_, Level p_312569_, BlockPos p_309943_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_312569_.getBlockEntity(p_309943_));
   }
}