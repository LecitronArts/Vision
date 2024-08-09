package net.minecraft.world.ticks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ContainerSingleItem extends Container {
   ItemStack getTheItem();

   ItemStack splitTheItem(int pAmount);

   void setTheItem(ItemStack pItem);

   BlockEntity getContainerBlockEntity();

   default ItemStack removeTheItem() {
      return this.splitTheItem(this.getMaxStackSize());
   }

   default int getContainerSize() {
      return 1;
   }

   default boolean isEmpty() {
      return this.getTheItem().isEmpty();
   }

   default void clearContent() {
      this.removeTheItem();
   }

   default ItemStack removeItemNoUpdate(int pSlot) {
      return this.removeItem(pSlot, this.getMaxStackSize());
   }

   default ItemStack getItem(int pSlot) {
      return pSlot == 0 ? this.getTheItem() : ItemStack.EMPTY;
   }

   default ItemStack removeItem(int pSlot, int pAmount) {
      return pSlot != 0 ? ItemStack.EMPTY : this.splitTheItem(pAmount);
   }

   default void setItem(int pSlot, ItemStack pStack) {
      if (pSlot == 0) {
         this.setTheItem(pStack);
      }

   }

   default boolean stillValid(Player pPlayer) {
      return Container.stillValidBlockEntity(this.getContainerBlockEntity(), pPlayer);
   }
}