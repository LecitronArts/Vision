package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CrafterBlockEntity extends RandomizableContainerBlockEntity implements CraftingContainer {
   public static final int CONTAINER_WIDTH = 3;
   public static final int CONTAINER_HEIGHT = 3;
   public static final int CONTAINER_SIZE = 9;
   public static final int SLOT_DISABLED = 1;
   public static final int SLOT_ENABLED = 0;
   public static final int DATA_TRIGGERED = 9;
   public static final int NUM_DATA = 10;
   private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
   private int craftingTicksRemaining = 0;
   protected final ContainerData containerData = new ContainerData() {
      private final int[] slotStates = new int[9];
      private int triggered = 0;

      public int get(int p_310435_) {
         return p_310435_ == 9 ? this.triggered : this.slotStates[p_310435_];
      }

      public void set(int p_313229_, int p_312585_) {
         if (p_313229_ == 9) {
            this.triggered = p_312585_;
         } else {
            this.slotStates[p_313229_] = p_312585_;
         }

      }

      public int getCount() {
         return 10;
      }
   };

   public CrafterBlockEntity(BlockPos pPos, BlockState pState) {
      super(BlockEntityType.CRAFTER, pPos, pState);
   }

   protected Component getDefaultName() {
      return Component.translatable("container.crafter");
   }

   protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
      return new CrafterMenu(pContainerId, pInventory, this, this.containerData);
   }

   public void setSlotState(int pSlot, boolean pState) {
      if (this.slotCanBeDisabled(pSlot)) {
         this.containerData.set(pSlot, pState ? 0 : 1);
         this.setChanged();
      }
   }

   public boolean isSlotDisabled(int pSlot) {
      if (pSlot >= 0 && pSlot < 9) {
         return this.containerData.get(pSlot) == 1;
      } else {
         return false;
      }
   }

   public boolean canPlaceItem(int pSlot, ItemStack pStack) {
      if (this.containerData.get(pSlot) == 1) {
         return false;
      } else {
         ItemStack itemstack = this.items.get(pSlot);
         int i = itemstack.getCount();
         if (i >= itemstack.getMaxStackSize()) {
            return false;
         } else if (itemstack.isEmpty()) {
            return true;
         } else {
            return !this.smallerStackExist(i, itemstack, pSlot);
         }
      }
   }

   private boolean smallerStackExist(int pCurrentSize, ItemStack pStack, int pSlot) {
      for(int i = pSlot + 1; i < 9; ++i) {
         if (!this.isSlotDisabled(i)) {
            ItemStack itemstack = this.getItem(i);
            if (itemstack.isEmpty() || itemstack.getCount() < pCurrentSize && ItemStack.isSameItemSameTags(itemstack, pStack)) {
               return true;
            }
         }
      }

      return false;
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.craftingTicksRemaining = pTag.getInt("crafting_ticks_remaining");
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(pTag)) {
         ContainerHelper.loadAllItems(pTag, this.items);
      }

      int[] aint = pTag.getIntArray("disabled_slots");

      for(int i = 0; i < 9; ++i) {
         this.containerData.set(i, 0);
      }

      for(int j : aint) {
         if (this.slotCanBeDisabled(j)) {
            this.containerData.set(j, 1);
         }
      }

      this.containerData.set(9, pTag.getInt("triggered"));
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      pTag.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);
      if (!this.trySaveLootTable(pTag)) {
         ContainerHelper.saveAllItems(pTag, this.items);
      }

      this.addDisabledSlots(pTag);
      this.addTriggered(pTag);
   }

   public int getContainerSize() {
      return 9;
   }

   public boolean isEmpty() {
      for(ItemStack itemstack : this.items) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int pIndex) {
      return this.items.get(pIndex);
   }

   public void setItem(int pIndex, ItemStack pStack) {
      if (this.isSlotDisabled(pIndex)) {
         this.setSlotState(pIndex, true);
      }

      super.setItem(pIndex, pStack);
   }

   public boolean stillValid(Player pPlayer) {
      if (this.level != null && this.level.getBlockEntity(this.worldPosition) == this) {
         return !(pPlayer.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) > 64.0D);
      } else {
         return false;
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> pItemStacks) {
      this.items = pItemStacks;
   }

   public int getWidth() {
      return 3;
   }

   public int getHeight() {
      return 3;
   }

   public void fillStackedContents(StackedContents pContents) {
      for(ItemStack itemstack : this.items) {
         pContents.accountSimpleStack(itemstack);
      }

   }

   private void addDisabledSlots(CompoundTag pTag) {
      IntList intlist = new IntArrayList();

      for(int i = 0; i < 9; ++i) {
         if (this.isSlotDisabled(i)) {
            intlist.add(i);
         }
      }

      pTag.putIntArray("disabled_slots", intlist);
   }

   private void addTriggered(CompoundTag pTag) {
      pTag.putInt("triggered", this.containerData.get(9));
   }

   public void setTriggered(boolean pTriggered) {
      this.containerData.set(9, pTriggered ? 1 : 0);
   }

   @VisibleForTesting
   public boolean isTriggered() {
      return this.containerData.get(9) == 1;
   }

   public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, CrafterBlockEntity pCrafter) {
      int i = pCrafter.craftingTicksRemaining - 1;
      if (i >= 0) {
         pCrafter.craftingTicksRemaining = i;
         if (i == 0) {
            pLevel.setBlock(pPos, pState.setValue(CrafterBlock.CRAFTING, Boolean.valueOf(false)), 3);
         }

      }
   }

   public void setCraftingTicksRemaining(int pCraftingTicksRemaining) {
      this.craftingTicksRemaining = pCraftingTicksRemaining;
   }

   public int getRedstoneSignal() {
      int i = 0;

      for(int j = 0; j < this.getContainerSize(); ++j) {
         ItemStack itemstack = this.getItem(j);
         if (!itemstack.isEmpty() || this.isSlotDisabled(j)) {
            ++i;
         }
      }

      return i;
   }

   private boolean slotCanBeDisabled(int pSlot) {
      return pSlot > -1 && pSlot < 9 && this.items.get(pSlot).isEmpty();
   }
}