package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements Clearable, ContainerSingleItem {
   private static final int SONG_END_PADDING = 20;
   private ItemStack item = ItemStack.EMPTY;
   private int ticksSinceLastEvent;
   private long tickCount;
   private long recordStartedTick;
   private boolean isPlaying;

   public JukeboxBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.JUKEBOX, pPos, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.contains("RecordItem", 10)) {
         this.item = ItemStack.of(pTag.getCompound("RecordItem"));
      }

      this.isPlaying = pTag.getBoolean("IsPlaying");
      this.recordStartedTick = pTag.getLong("RecordStartTick");
      this.tickCount = pTag.getLong("TickCount");
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (!this.getTheItem().isEmpty()) {
         pTag.put("RecordItem", this.getTheItem().save(new CompoundTag()));
      }

      pTag.putBoolean("IsPlaying", this.isPlaying);
      pTag.putLong("RecordStartTick", this.recordStartedTick);
      pTag.putLong("TickCount", this.tickCount);
   }

   public boolean isRecordPlaying() {
      return !this.getTheItem().isEmpty() && this.isPlaying;
   }

   private void setHasRecordBlockState(@Nullable Entity pEntity, boolean pHasRecord) {
      if (this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
         this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, Boolean.valueOf(pHasRecord)), 2);
         this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(pEntity, this.getBlockState()));
      }

   }

   @VisibleForTesting
   public void startPlaying() {
      this.recordStartedTick = this.tickCount;
      this.isPlaying = true;
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.level.levelEvent((Player)null, 1010, this.getBlockPos(), Item.getId(this.getTheItem().getItem()));
      this.setChanged();
   }

   private void stopPlaying() {
      this.isPlaying = false;
      this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.level.levelEvent(1011, this.getBlockPos(), 0);
      this.setChanged();
   }

   private void tick(Level pLevel, BlockPos pPos, BlockState pState) {
      ++this.ticksSinceLastEvent;
      if (this.isRecordPlaying()) {
         Item item = this.getTheItem().getItem();
         if (item instanceof RecordItem) {
            RecordItem recorditem = (RecordItem)item;
            if (this.shouldRecordStopPlaying(recorditem)) {
               this.stopPlaying();
            } else if (this.shouldSendJukeboxPlayingEvent()) {
               this.ticksSinceLastEvent = 0;
               pLevel.gameEvent(GameEvent.JUKEBOX_PLAY, pPos, GameEvent.Context.of(pState));
               this.spawnMusicParticles(pLevel, pPos);
            }
         }
      }

      ++this.tickCount;
   }

   private boolean shouldRecordStopPlaying(RecordItem pRecord) {
      return this.tickCount >= this.recordStartedTick + (long)pRecord.getLengthInTicks() + 20L;
   }

   private boolean shouldSendJukeboxPlayingEvent() {
      return this.ticksSinceLastEvent >= 20;
   }

   public ItemStack getTheItem() {
      return this.item;
   }

   public ItemStack splitTheItem(int pAmount) {
      ItemStack itemstack = this.item;
      this.item = ItemStack.EMPTY;
      if (!itemstack.isEmpty()) {
         this.setHasRecordBlockState((Entity)null, false);
         this.stopPlaying();
      }

      return itemstack;
   }

   public void setTheItem(ItemStack pItem) {
      if (pItem.is(ItemTags.MUSIC_DISCS) && this.level != null) {
         this.item = pItem;
         this.setHasRecordBlockState((Entity)null, true);
         this.startPlaying();
      } else if (pItem.isEmpty()) {
         this.splitTheItem(1);
      }

   }

   public int getMaxStackSize() {
      return 1;
   }

   public BlockEntity getContainerBlockEntity() {
      return this;
   }

   public boolean canPlaceItem(int pSlot, ItemStack pStack) {
      return pStack.is(ItemTags.MUSIC_DISCS) && this.getItem(pSlot).isEmpty();
   }

   public boolean canTakeItem(Container pTarget, int pSlot, ItemStack pStack) {
      return pTarget.hasAnyMatching(ItemStack::isEmpty);
   }

   private void spawnMusicParticles(Level pLevel, BlockPos pPos) {
      if (pLevel instanceof ServerLevel serverlevel) {
         Vec3 vec3 = Vec3.atBottomCenterOf(pPos).add(0.0D, (double)1.2F, 0.0D);
         float f = (float)pLevel.getRandom().nextInt(4) / 24.0F;
         serverlevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, (double)f, 0.0D, 0.0D, 1.0D);
      }

   }

   public void popOutRecord() {
      if (this.level != null && !this.level.isClientSide) {
         BlockPos blockpos = this.getBlockPos();
         ItemStack itemstack = this.getTheItem();
         if (!itemstack.isEmpty()) {
            this.removeTheItem();
            Vec3 vec3 = Vec3.atLowerCornerWithOffset(blockpos, 0.5D, 1.01D, 0.5D).offsetRandom(this.level.random, 0.7F);
            ItemStack itemstack1 = itemstack.copy();
            ItemEntity itementity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemstack1);
            itementity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itementity);
         }
      }
   }

   public static void playRecordTick(Level pLevel, BlockPos pPos, BlockState pState, JukeboxBlockEntity pJukebox) {
      pJukebox.tick(pLevel, pPos, pState);
   }

   @VisibleForTesting
   public void setRecordWithoutPlaying(ItemStack pStack) {
      this.item = pStack;
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.setChanged();
   }
}