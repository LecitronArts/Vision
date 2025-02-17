package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsTermsScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.terms.title");
   private static final Component TERMS_STATIC_TEXT = Component.translatable("mco.terms.sentence.1");
   private static final Component TERMS_LINK_TEXT = CommonComponents.space().append(Component.translatable("mco.terms.sentence.2").withStyle(Style.EMPTY.withUnderlined(true)));
   private final Screen lastScreen;
   private final RealmsServer realmsServer;
   private boolean onLink;

   public RealmsTermsScreen(Screen pLastScreen, RealmsServer pRealmsServer) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.realmsServer = pRealmsServer;
   }

   public void init() {
      int i = this.width / 4 - 2;
      this.addRenderableWidget(Button.builder(Component.translatable("mco.terms.buttons.agree"), (p_90054_) -> {
         this.agreedToTos();
      }).bounds(this.width / 4, row(12), i, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("mco.terms.buttons.disagree"), (p_280762_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).bounds(this.width / 2 + 4, row(12), i, 20).build());
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private void agreedToTos() {
      RealmsClient realmsclient = RealmsClient.create();

      try {
         realmsclient.agreeToTos();
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new GetServerDetailsTask(this.lastScreen, this.realmsServer)));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't agree to TOS", (Throwable)realmsserviceexception);
      }

   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.onLink) {
         this.minecraft.keyboardHandler.setClipboard("https://aka.ms/MinecraftRealmsTerms");
         Util.getPlatform().openUri("https://aka.ms/MinecraftRealmsTerms");
         return true;
      } else {
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), TERMS_STATIC_TEXT).append(CommonComponents.SPACE).append(TERMS_LINK_TEXT);
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      pGuiGraphics.drawString(this.font, TERMS_STATIC_TEXT, this.width / 2 - 120, row(5), -1, false);
      int i = this.font.width(TERMS_STATIC_TEXT);
      int j = this.width / 2 - 121 + i;
      int k = row(5);
      int l = j + this.font.width(TERMS_LINK_TEXT) + 1;
      int i1 = k + 1 + 9;
      this.onLink = j <= pMouseX && pMouseX <= l && k <= pMouseY && pMouseY <= i1;
      pGuiGraphics.drawString(this.font, TERMS_LINK_TEXT, this.width / 2 - 120 + i, row(5), this.onLink ? 7107012 : 3368635, false);
   }
}