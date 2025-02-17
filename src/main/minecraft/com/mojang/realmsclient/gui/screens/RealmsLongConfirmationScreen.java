package com.mojang.realmsclient.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsLongConfirmationScreen extends RealmsScreen {
   static final Component WARNING = Component.translatable("mco.warning");
   static final Component INFO = Component.translatable("mco.info");
   private final RealmsLongConfirmationScreen.Type type;
   private final Component line2;
   private final Component line3;
   protected final BooleanConsumer callback;
   private final boolean yesNoQuestion;

   public RealmsLongConfirmationScreen(BooleanConsumer pConsumer, RealmsLongConfirmationScreen.Type pType, Component pLine2, Component pLine3, boolean pYesNoQuestion) {
      super(GameNarrator.NO_TITLE);
      this.callback = pConsumer;
      this.type = pType;
      this.line2 = pLine2;
      this.line3 = pLine3;
      this.yesNoQuestion = pYesNoQuestion;
   }

   public void init() {
      if (this.yesNoQuestion) {
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_YES, (p_88751_) -> {
            this.callback.accept(true);
         }).bounds(this.width / 2 - 105, row(8), 100, 20).build());
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_NO, (p_88749_) -> {
            this.callback.accept(false);
         }).bounds(this.width / 2 + 5, row(8), 100, 20).build());
      } else {
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (p_88746_) -> {
            this.callback.accept(true);
         }).bounds(this.width / 2 - 50, row(8), 100, 20).build());
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinLines(this.type.text, this.line2, this.line3);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.type.text, this.width / 2, row(2), this.type.colorCode);
      pGuiGraphics.drawCenteredString(this.font, this.line2, this.width / 2, row(4), -1);
      pGuiGraphics.drawCenteredString(this.font, this.line3, this.width / 2, row(6), -1);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      WARNING(RealmsLongConfirmationScreen.WARNING, -65536),
      INFO(RealmsLongConfirmationScreen.INFO, 8226750);

      public final int colorCode;
      public final Component text;

      private Type(Component pText, int pColorCode) {
         this.text = pText;
         this.colorCode = pColorCode;
      }
   }
}