package com.mojang.realmsclient.gui.screens;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsClientOutdatedScreen extends RealmsScreen {
   private static final Component INCOMPATIBLE_TITLE = Component.translatable("mco.client.incompatible.title");
   private static final Component[] INCOMPATIBLE_MESSAGES_SNAPSHOT = new Component[]{Component.translatable("mco.client.incompatible.msg.line1"), Component.translatable("mco.client.incompatible.msg.line2"), Component.translatable("mco.client.incompatible.msg.line3")};
   private static final Component[] INCOMPATIBLE_MESSAGES = new Component[]{Component.translatable("mco.client.incompatible.msg.line1"), Component.translatable("mco.client.incompatible.msg.line2")};
   private final Screen lastScreen;

   public RealmsClientOutdatedScreen(Screen pLastScreen) {
      super(INCOMPATIBLE_TITLE);
      this.lastScreen = pLastScreen;
   }

   public void init() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_280710_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).bounds(this.width / 2 - 100, row(12), 200, 20).build());
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, row(3), -65536);
      Component[] acomponent = this.getMessages();

      for(int i = 0; i < acomponent.length; ++i) {
         pGuiGraphics.drawCenteredString(this.font, acomponent[i], this.width / 2, row(5) + i * 12, -1);
      }

   }

   private Component[] getMessages() {
      return SharedConstants.getCurrentVersion().isStable() ? INCOMPATIBLE_MESSAGES : INCOMPATIBLE_MESSAGES_SNAPSHOT;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode != 257 && pKeyCode != 335 && pKeyCode != 256) {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      } else {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      }
   }
}