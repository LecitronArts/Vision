package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancements.Listener {
   private static final ResourceLocation WINDOW_LOCATION = new ResourceLocation("textures/gui/advancements/window.png");
   public static final int WINDOW_WIDTH = 252;
   public static final int WINDOW_HEIGHT = 140;
   private static final int WINDOW_INSIDE_X = 9;
   private static final int WINDOW_INSIDE_Y = 18;
   public static final int WINDOW_INSIDE_WIDTH = 234;
   public static final int WINDOW_INSIDE_HEIGHT = 113;
   private static final int WINDOW_TITLE_X = 8;
   private static final int WINDOW_TITLE_Y = 6;
   public static final int BACKGROUND_TILE_WIDTH = 16;
   public static final int BACKGROUND_TILE_HEIGHT = 16;
   public static final int BACKGROUND_TILE_COUNT_X = 14;
   public static final int BACKGROUND_TILE_COUNT_Y = 7;
   private static final double SCROLL_SPEED = 16.0D;
   private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
   private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
   private static final Component TITLE = Component.translatable("gui.advancements");
   private final ClientAdvancements advancements;
   private final Map<AdvancementHolder, AdvancementTab> tabs = Maps.newLinkedHashMap();
   @Nullable
   private AdvancementTab selectedTab;
   private boolean isScrolling;

   public AdvancementsScreen(ClientAdvancements pAdvancements) {
      super(GameNarrator.NO_TITLE);
      this.advancements = pAdvancements;
   }

   protected void init() {
      this.tabs.clear();
      this.selectedTab = null;
      this.advancements.setListener(this);
      if (this.selectedTab == null && !this.tabs.isEmpty()) {
         AdvancementTab advancementtab = this.tabs.values().iterator().next();
         this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
      } else {
         this.advancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
      }

   }

   public void removed() {
      this.advancements.setListener((ClientAdvancements.Listener)null);
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
      }

   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         int i = (this.width - 252) / 2;
         int j = (this.height - 140) / 2;

         for(AdvancementTab advancementtab : this.tabs.values()) {
            if (advancementtab.isMouseOver(i, j, pMouseX, pMouseY)) {
               this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
               break;
            }
         }
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.minecraft.options.keyAdvancements.matches(pKeyCode, pScanCode)) {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      int i = (this.width - 252) / 2;
      int j = (this.height - 140) / 2;
      this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.renderInside(pGuiGraphics, pMouseX, pMouseY, i, j);
      this.renderWindow(pGuiGraphics, i, j);
      this.renderTooltips(pGuiGraphics, pMouseX, pMouseY, i, j);
   }

   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (pButton != 0) {
         this.isScrolling = false;
         return false;
      } else {
         if (!this.isScrolling) {
            this.isScrolling = true;
         } else if (this.selectedTab != null) {
            this.selectedTab.scroll(pDragX, pDragY);
         }

         return true;
      }
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      if (this.selectedTab != null) {
         this.selectedTab.scroll(pScrollX * 16.0D, pScrollY * 16.0D);
         return true;
      } else {
         return false;
      }
   }

   private void renderInside(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pOffsetX, int pOffsetY) {
      AdvancementTab advancementtab = this.selectedTab;
      if (advancementtab == null) {
         pGuiGraphics.fill(pOffsetX + 9, pOffsetY + 18, pOffsetX + 9 + 234, pOffsetY + 18 + 113, -16777216);
         int i = pOffsetX + 9 + 117;
         pGuiGraphics.drawCenteredString(this.font, NO_ADVANCEMENTS_LABEL, i, pOffsetY + 18 + 56 - 9 / 2, -1);
         pGuiGraphics.drawCenteredString(this.font, VERY_SAD_LABEL, i, pOffsetY + 18 + 113 - 9, -1);
      } else {
         advancementtab.drawContents(pGuiGraphics, pOffsetX + 9, pOffsetY + 18);
      }
   }

   public void renderWindow(GuiGraphics pGuiGraphics, int pOffsetX, int pOffsetY) {
      RenderSystem.enableBlend();
      pGuiGraphics.blit(WINDOW_LOCATION, pOffsetX, pOffsetY, 0, 0, 252, 140);
      if (this.tabs.size() > 1) {
         for(AdvancementTab advancementtab : this.tabs.values()) {
            advancementtab.drawTab(pGuiGraphics, pOffsetX, pOffsetY, advancementtab == this.selectedTab);
         }

         for(AdvancementTab advancementtab1 : this.tabs.values()) {
            advancementtab1.drawIcon(pGuiGraphics, pOffsetX, pOffsetY);
         }
      }

      pGuiGraphics.drawString(this.font, TITLE, pOffsetX + 8, pOffsetY + 6, 4210752, false);
   }

   private void renderTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pOffsetX, int pOffsetY) {
      if (this.selectedTab != null) {
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate((float)(pOffsetX + 9), (float)(pOffsetY + 18), 400.0F);
         RenderSystem.enableDepthTest();
         this.selectedTab.drawTooltips(pGuiGraphics, pMouseX - pOffsetX - 9, pMouseY - pOffsetY - 18, pOffsetX, pOffsetY);
         RenderSystem.disableDepthTest();
         pGuiGraphics.pose().popPose();
      }

      if (this.tabs.size() > 1) {
         for(AdvancementTab advancementtab : this.tabs.values()) {
            if (advancementtab.isMouseOver(pOffsetX, pOffsetY, (double)pMouseX, (double)pMouseY)) {
               pGuiGraphics.renderTooltip(this.font, advancementtab.getTitle(), pMouseX, pMouseY);
            }
         }
      }

   }

   public void onAddAdvancementRoot(AdvancementNode pAdvancement) {
      AdvancementTab advancementtab = AdvancementTab.create(this.minecraft, this, this.tabs.size(), pAdvancement);
      if (advancementtab != null) {
         this.tabs.put(pAdvancement.holder(), advancementtab);
      }
   }

   public void onRemoveAdvancementRoot(AdvancementNode pAdvancement) {
   }

   public void onAddAdvancementTask(AdvancementNode pAdvancement) {
      AdvancementTab advancementtab = this.getTab(pAdvancement);
      if (advancementtab != null) {
         advancementtab.addAdvancement(pAdvancement);
      }

   }

   public void onRemoveAdvancementTask(AdvancementNode pAdvancement) {
   }

   public void onUpdateAdvancementProgress(AdvancementNode pAdvancement, AdvancementProgress pAdvancementProgress) {
      AdvancementWidget advancementwidget = this.getAdvancementWidget(pAdvancement);
      if (advancementwidget != null) {
         advancementwidget.setProgress(pAdvancementProgress);
      }

   }

   public void onSelectedTabChanged(@Nullable AdvancementHolder pAdvancement) {
      this.selectedTab = this.tabs.get(pAdvancement);
   }

   public void onAdvancementsCleared() {
      this.tabs.clear();
      this.selectedTab = null;
   }

   @Nullable
   public AdvancementWidget getAdvancementWidget(AdvancementNode pAdvancement) {
      AdvancementTab advancementtab = this.getTab(pAdvancement);
      return advancementtab == null ? null : advancementtab.getWidget(pAdvancement.holder());
   }

   @Nullable
   private AdvancementTab getTab(AdvancementNode pAdvancement) {
      AdvancementNode advancementnode = pAdvancement.root();
      return this.tabs.get(advancementnode.holder());
   }
}