package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RandomizableContainerBlockEntity extends BaseContainerBlockEntity implements RandomizableContainer {
   @Nullable
   protected ResourceLocation lootTable;
   protected long lootTableSeed;

   protected RandomizableContainerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
      super(pType, pPos, pBlockState);
   }

   @Nullable
   public ResourceLocation getLootTable() {
      return this.lootTable;
   }

   public void setLootTable(@Nullable ResourceLocation pLootTable) {
      this.lootTable = pLootTable;
   }

   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   public void setLootTableSeed(long pSeed) {
      this.lootTableSeed = pSeed;
   }

   public boolean isEmpty() {
      this.unpackLootTable((Player)null);
      return this.getItems().stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int pIndex) {
      this.unpackLootTable((Player)null);
      return this.getItems().get(pIndex);
   }

   public ItemStack removeItem(int pIndex, int pCount) {
      this.unpackLootTable((Player)null);
      ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), pIndex, pCount);
      if (!itemstack.isEmpty()) {
         this.setChanged();
      }

      return itemstack;
   }

   public ItemStack removeItemNoUpdate(int pIndex) {
      this.unpackLootTable((Player)null);
      return ContainerHelper.takeItem(this.getItems(), pIndex);
   }

   public void setItem(int pIndex, ItemStack pStack) {
      this.unpackLootTable((Player)null);
      this.getItems().set(pIndex, pStack);
      if (pStack.getCount() > this.getMaxStackSize()) {
         pStack.setCount(this.getMaxStackSize());
      }

      this.setChanged();
   }

   public boolean stillValid(Player pPlayer) {
      return Container.stillValidBlockEntity(this, pPlayer);
   }

   public void clearContent() {
      this.getItems().clear();
   }

   protected abstract NonNullList<ItemStack> getItems();

   protected abstract void setItems(NonNullList<ItemStack> pItemStacks);

   public boolean canOpen(Player pPlayer) {
      return super.canOpen(pPlayer) && (this.lootTable == null || !pPlayer.isSpectator());
   }

   @Nullable
   public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
      if (this.canOpen(pPlayer)) {
         this.unpackLootTable(pPlayerInventory.player);
         return this.createMenu(pContainerId, pPlayerInventory);
      } else {
         return null;
      }
   }
}