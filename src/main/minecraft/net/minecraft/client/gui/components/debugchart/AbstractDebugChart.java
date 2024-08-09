package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.SampleLogger;

public abstract class AbstractDebugChart {
   protected static final int COLOR_GREY = 14737632;
   protected static final int CHART_HEIGHT = 60;
   protected static final int LINE_WIDTH = 1;
   protected final Font font;
   protected final SampleLogger logger;

   protected AbstractDebugChart(Font pFont, SampleLogger pLogger) {
      this.font = pFont;
      this.logger = pLogger;
   }

   public int getWidth(int pMaxWidth) {
      return Math.min(this.logger.capacity() + 2, pMaxWidth);
   }

   public void drawChart(GuiGraphics pGuiGraphics, int pX, int pWidth) {
      if (this instanceof TpsDebugChart) {
         Minecraft minecraft = Minecraft.getInstance();
         int i = (int)(512.0D / minecraft.getWindow().getGuiScale());
         pX = Math.max(pX, i);
         pWidth = minecraft.getWindow().getGuiScaledWidth() - pX;
      }

      int k2 = pGuiGraphics.guiHeight();
      pGuiGraphics.fill(RenderType.guiOverlay(), pX, k2 - 60, pX + pWidth, k2, -1873784752);
      long l2 = 0L;
      long j = 2147483647L;
      long k = -2147483648L;
      int l = Math.max(0, this.logger.capacity() - (pWidth - 2));
      int i1 = this.logger.size() - l;

      for(int j1 = 0; j1 < i1; ++j1) {
         int k1 = pX + j1 + 1;
         long l1 = this.logger.get(l + j1);
         j = Math.min(j, l1);
         k = Math.max(k, l1);
         l2 += l1;
         int i2 = this.getSampleHeight((double)l1);
         int j2 = this.getSampleColor(l1);
         pGuiGraphics.fill(RenderType.guiOverlay(), k1, k2 - i2, k1 + 1, k2, j2);
      }

      pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, k2 - 60, -1);
      pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, k2 - 1, -1);
      pGuiGraphics.vLine(RenderType.guiOverlay(), pX, k2 - 60, k2, -1);
      pGuiGraphics.vLine(RenderType.guiOverlay(), pX + pWidth - 1, k2 - 60, k2, -1);
      if (i1 > 0) {
         String s = this.toDisplayString((double)j) + " min";
         String s1 = this.toDisplayString((double)l2 / (double)i1) + " avg";
         String s2 = this.toDisplayString((double)k) + " max";
         pGuiGraphics.drawString(this.font, s, pX + 2, k2 - 60 - 9, 14737632);
         pGuiGraphics.drawCenteredString(this.font, s1, pX + pWidth / 2, k2 - 60 - 9, 14737632);
         pGuiGraphics.drawString(this.font, s2, pX + pWidth - this.font.width(s2) - 2, k2 - 60 - 9, 14737632);
      }

      this.renderAdditionalLinesAndLabels(pGuiGraphics, pX, pWidth, k2);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
   }

   protected void drawStringWithShade(GuiGraphics pGuiGraphics, String pText, int pX, int pY) {
      pGuiGraphics.fill(RenderType.guiOverlay(), pX, pY, pX + this.font.width(pText) + 1, pY + 9, -1873784752);
      pGuiGraphics.drawString(this.font, pText, pX + 1, pY + 1, 14737632, false);
   }

   protected abstract String toDisplayString(double pValue);

   protected abstract int getSampleHeight(double pValue);

   protected abstract int getSampleColor(long pValue);

   protected int getSampleColor(double pValue, double pMinPosition, int pMinColor, double pMidPosition, int pMidColor, double pMaxPosition, int pGuiGraphics) {
      pValue = Mth.clamp(pValue, pMinPosition, pMaxPosition);
      return pValue < pMidPosition ? FastColor.ARGB32.lerp((float)(pValue / (pMidPosition - pMinPosition)), pMinColor, pMidColor) : FastColor.ARGB32.lerp((float)((pValue - pMidPosition) / (pMaxPosition - pMidPosition)), pMidColor, pGuiGraphics);
   }
}