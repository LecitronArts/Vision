package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnvilScreen extends ItemCombinerScreen<AnvilMenu> {
   private static final ResourceLocation TEXT_FIELD_SPRITE = new ResourceLocation("container/anvil/text_field");
   private static final ResourceLocation TEXT_FIELD_DISABLED_SPRITE = new ResourceLocation("container/anvil/text_field_disabled");
   private static final ResourceLocation ERROR_SPRITE = new ResourceLocation("container/anvil/error");
   private static final ResourceLocation ANVIL_LOCATION = new ResourceLocation("textures/gui/container/anvil.png");
   private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
   private EditBox name;
   private final Player player;

   public AnvilScreen(AnvilMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
      super(pMenu, pPlayerInventory, pTitle, ANVIL_LOCATION);
      this.player = pPlayerInventory.player;
      this.titleLabelX = 60;
   }

   protected void subInit() {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      this.name = new EditBox(this.font, i + 62, j + 24, 103, 12, Component.translatable("container.repair"));
      this.name.setCanLoseFocus(false);
      this.name.setTextColor(-1);
      this.name.setTextColorUneditable(-1);
      this.name.setBordered(false);
      this.name.setMaxLength(50);
      this.name.setResponder(this::onNameChanged);
      this.name.setValue("");
      this.addWidget(this.name);
      this.setInitialFocus(this.name);
      this.name.setEditable(this.menu.getSlot(0).hasItem());
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.name.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.name.setValue(s);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.player.closeContainer();
      }

      return !this.name.keyPressed(pKeyCode, pScanCode, pModifiers) && !this.name.canConsumeInput() ? super.keyPressed(pKeyCode, pScanCode, pModifiers) : true;
   }

   private void onNameChanged(String p_97899_) {
      Slot slot = this.menu.getSlot(0);
      if (slot.hasItem()) {
         String s = p_97899_;
         if (!slot.getItem().hasCustomHoverName() && p_97899_.equals(slot.getItem().getHoverName().getString())) {
            s = "";
         }

         if (this.menu.setItemName(s)) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
         }

      }
   }

   protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
      super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
      int i = this.menu.getCost();
      if (i > 0) {
         int j = 8453920;
         Component component;
         if (i >= 40 && !this.minecraft.player.getAbilities().instabuild) {
            component = TOO_EXPENSIVE_TEXT;
            j = 16736352;
         } else if (!this.menu.getSlot(2).hasItem()) {
            component = null;
         } else {
            component = Component.translatable("container.repair.cost", i);
            if (!this.menu.getSlot(2).mayPickup(this.player)) {
               j = 16736352;
            }
         }

         if (component != null) {
            int k = this.imageWidth - 8 - this.font.width(component) - 2;
            int l = 69;
            pGuiGraphics.fill(k - 2, 67, this.imageWidth - 8, 79, 1325400064);
            pGuiGraphics.drawString(this.font, component, k, 69, j);
         }
      }

   }

   protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
      super.renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
      pGuiGraphics.blitSprite(this.menu.getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE, this.leftPos + 59, this.topPos + 20, 110, 16);
   }

   public void renderFg(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.name.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }

   protected void renderErrorIcon(GuiGraphics pGuiGraphics, int pX, int pY) {
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()) {
         pGuiGraphics.blitSprite(ERROR_SPRITE, pX + 99, pY + 45, 28, 21);
      }

   }

   public void slotChanged(AbstractContainerMenu pContainerToSend, int pSlotInd, ItemStack pStack) {
      if (pSlotInd == 0) {
         this.name.setValue(pStack.isEmpty() ? "" : pStack.getHoverName().getString());
         this.name.setEditable(!pStack.isEmpty());
         this.setFocused(this.name);
      }

   }
}