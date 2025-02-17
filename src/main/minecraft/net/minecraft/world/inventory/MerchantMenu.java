package net.minecraft.world.inventory;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantMenu extends AbstractContainerMenu {
   protected static final int PAYMENT1_SLOT = 0;
   protected static final int PAYMENT2_SLOT = 1;
   protected static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private static final int SELLSLOT1_X = 136;
   private static final int SELLSLOT2_X = 162;
   private static final int BUYSLOT_X = 220;
   private static final int ROW_Y = 37;
   private final Merchant trader;
   private final MerchantContainer tradeContainer;
   private int merchantLevel;
   private boolean showProgressBar;
   private boolean canRestock;

   public MerchantMenu(int pContainerId, Inventory pPlayerInventory) {
      this(pContainerId, pPlayerInventory, new ClientSideMerchant(pPlayerInventory.player));
   }

   public MerchantMenu(int pContainerId, Inventory pPlayerInventory, Merchant pTrader) {
      super(MenuType.MERCHANT, pContainerId);
      this.trader = pTrader;
      this.tradeContainer = new MerchantContainer(pTrader);
      this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
      this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
      this.addSlot(new MerchantResultSlot(pPlayerInventory.player, pTrader, this.tradeContainer, 2, 220, 37));

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(pPlayerInventory, j + i * 9 + 9, 108 + j * 18, 84 + i * 18));
         }
      }

      for(int k = 0; k < 9; ++k) {
         this.addSlot(new Slot(pPlayerInventory, k, 108 + k * 18, 142));
      }

   }

   public void setShowProgressBar(boolean pShowProgressBar) {
      this.showProgressBar = pShowProgressBar;
   }

   public void slotsChanged(Container pInventory) {
      this.tradeContainer.updateSellItem();
      super.slotsChanged(pInventory);
   }

   public void setSelectionHint(int pCurrentRecipeIndex) {
      this.tradeContainer.setSelectionHint(pCurrentRecipeIndex);
   }

   public boolean stillValid(Player pPlayer) {
      return this.trader.getTradingPlayer() == pPlayer;
   }

   public int getTraderXp() {
      return this.trader.getVillagerXp();
   }

   public int getFutureTraderXp() {
      return this.tradeContainer.getFutureXp();
   }

   public void setXp(int pXp) {
      this.trader.overrideXp(pXp);
   }

   public int getTraderLevel() {
      return this.merchantLevel;
   }

   public void setMerchantLevel(int pLevel) {
      this.merchantLevel = pLevel;
   }

   public void setCanRestock(boolean pCanRestock) {
      this.canRestock = pCanRestock;
   }

   public boolean canRestock() {
      return this.canRestock;
   }

   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2);
   }

   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(pIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (pIndex == 2) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
            this.playTradeSound();
         } else if (pIndex != 0 && pIndex != 1) {
            if (pIndex >= 3 && pIndex < 30) {
               if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= 30 && pIndex < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(pPlayer, itemstack1);
      }

      return itemstack;
   }

   private void playTradeSound() {
      if (!this.trader.isClientSide()) {
         Entity entity = (Entity)this.trader;
         entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
      }

   }

   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.trader.setTradingPlayer((Player)null);
      if (!this.trader.isClientSide()) {
         if (!pPlayer.isAlive() || pPlayer instanceof ServerPlayer && ((ServerPlayer)pPlayer).hasDisconnected()) {
            ItemStack itemstack = this.tradeContainer.removeItemNoUpdate(0);
            if (!itemstack.isEmpty()) {
               pPlayer.drop(itemstack, false);
            }

            itemstack = this.tradeContainer.removeItemNoUpdate(1);
            if (!itemstack.isEmpty()) {
               pPlayer.drop(itemstack, false);
            }
         } else if (pPlayer instanceof ServerPlayer) {
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(0));
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(1));
         }

      }
   }

   public void tryMoveItems(int pSelectedMerchantRecipe) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
         //ci.cancel();

         if (pSelectedMerchantRecipe >= this.getOffers().size()) return;

         final MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
         final LocalPlayer player = Minecraft.getInstance().player;

         // move 1st input slot to inventory
         if (!this.tradeContainer.getItem(0).isEmpty()) {
            final int count = this.tradeContainer.getItem(0).getCount();
            interactionManager.handleInventoryMouseClick(containerId, 0, 0, ClickType.QUICK_MOVE, player);
            if (count == this.tradeContainer.getItem(0).getCount()) return;
         }

         // move 2nd input slot to inventory
         if (!this.tradeContainer.getItem(1).isEmpty()) {
            final int count = this.tradeContainer.getItem(1).getCount();
            interactionManager.handleInventoryMouseClick(containerId, 1, 0, ClickType.QUICK_MOVE, player);
            if (count == this.tradeContainer.getItem(1).getCount()) return;
         }

         // refill the slots
         if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            this.viaFabricPlus$autofill(interactionManager, player, 0, this.getOffers().get(pSelectedMerchantRecipe).getCostA());
            this.viaFabricPlus$autofill(interactionManager, player, 1, this.getOffers().get(pSelectedMerchantRecipe).getCostB());
         }
      } else if (pSelectedMerchantRecipe >= 0 && this.getOffers().size() > pSelectedMerchantRecipe) {
         ItemStack itemstack = this.tradeContainer.getItem(0);
         if (!itemstack.isEmpty()) {
            if (!this.moveItemStackTo(itemstack, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(0, itemstack);
         }

         ItemStack itemstack1 = this.tradeContainer.getItem(1);
         if (!itemstack1.isEmpty()) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(1, itemstack1);
         }

         if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            ItemStack itemstack2 = this.getOffers().get(pSelectedMerchantRecipe).getCostA();
            this.moveFromInventoryToPaymentSlot(0, itemstack2);
            ItemStack itemstack3 = this.getOffers().get(pSelectedMerchantRecipe).getCostB();
            this.moveFromInventoryToPaymentSlot(1, itemstack3);
         }

      }
   }

   private void moveFromInventoryToPaymentSlot(int pPaymentSlotIndex, ItemStack pPaymentSlot) {
      if (!pPaymentSlot.isEmpty()) {
         for(int i = 3; i < 39; ++i) {
            ItemStack itemstack = this.slots.get(i).getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(pPaymentSlot, itemstack)) {
               ItemStack itemstack1 = this.tradeContainer.getItem(pPaymentSlotIndex);
               int j = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
               int k = Math.min(pPaymentSlot.getMaxStackSize() - j, itemstack.getCount());
               ItemStack itemstack2 = itemstack.copy();
               int l = j + k;
               itemstack.shrink(k);
               itemstack2.setCount(l);
               this.tradeContainer.setItem(pPaymentSlotIndex, itemstack2);
               if (l >= pPaymentSlot.getMaxStackSize()) {
                  break;
               }
            }
         }
      }

   }
   private void viaFabricPlus$autofill(MultiPlayerGameMode interactionManager, LocalPlayer player, int inputSlot, ItemStack stackNeeded) {
      if (stackNeeded.isEmpty()) return;

      int slot;
      for (slot = 3; slot < 39; slot++) {
         final ItemStack stack = slots.get(slot).getItem();
         if (ItemStack.isSameItemSameTags(stack, stackNeeded)) {
            break;
         }
      }
      if (slot == 39) return;

      final boolean wasHoldingItem = !player.containerMenu.getCarried().isEmpty();
      interactionManager.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player);
      interactionManager.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP_ALL, player);
      interactionManager.handleInventoryMouseClick(containerId, inputSlot, 0, ClickType.PICKUP, player);
      if (wasHoldingItem) interactionManager.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player);
   }

   public void setOffers(MerchantOffers pOffers) {
      this.trader.overrideOffers(pOffers);
   }

   public MerchantOffers getOffers() {
      return this.trader.getOffers();
   }

   public boolean showProgressBar() {
      return this.showProgressBar;
   }
}