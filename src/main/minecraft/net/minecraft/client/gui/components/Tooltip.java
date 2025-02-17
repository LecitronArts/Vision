package net.minecraft.client.gui.components;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Tooltip implements NarrationSupplier {
   private static final int MAX_WIDTH = 170;
   private final Component message;
   @Nullable
   private List<FormattedCharSequence> cachedTooltip;
   @Nullable
   private final Component narration;
   private int msDelay;
   private long hoverOrFocusedStartTime;
   private boolean wasHoveredOrFocused;

   private Tooltip(Component pMessage, @Nullable Component pNarration) {
      this.message = pMessage;
      this.narration = pNarration;
   }

   public void setDelay(int pDelay) {
      this.msDelay = pDelay;
   }

   public static Tooltip create(Component pMessage, @Nullable Component pNarration) {
      return new Tooltip(pMessage, pNarration);
   }

   public static Tooltip create(Component pMessage) {
      return new Tooltip(pMessage, pMessage);
   }

   public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
      if (this.narration != null) {
         pNarrationElementOutput.add(NarratedElementType.HINT, this.narration);
      }

   }

   public List<FormattedCharSequence> toCharSequence(Minecraft pMinecraft) {
      if (this.cachedTooltip == null) {
         this.cachedTooltip = splitTooltip(pMinecraft, this.message);
      }

      return this.cachedTooltip;
   }

   public static List<FormattedCharSequence> splitTooltip(Minecraft pMinecraft, Component pMessage) {
      return pMinecraft.font.split(pMessage, 170);
   }

   public void refreshTooltipForNextRenderPass(boolean pHovering, boolean pFocused, ScreenRectangle pScreenRectangle) {
      boolean flag = pHovering || pFocused && Minecraft.getInstance().getLastInputType().isKeyboard();
      if (flag != this.wasHoveredOrFocused) {
         if (flag) {
            this.hoverOrFocusedStartTime = Util.getMillis();
         }

         this.wasHoveredOrFocused = flag;
      }

      if (flag && Util.getMillis() - this.hoverOrFocusedStartTime > (long)this.msDelay) {
         Screen screen = Minecraft.getInstance().screen;
         if (screen != null) {
            screen.setTooltipForNextRenderPass(this, this.createTooltipPositioner(pHovering, pFocused, pScreenRectangle), pFocused);
         }
      }

   }

   protected ClientTooltipPositioner createTooltipPositioner(boolean pHovering, boolean pFocused, ScreenRectangle pScreenRectangle) {
      return (ClientTooltipPositioner)(!pHovering && pFocused && Minecraft.getInstance().getLastInputType().isKeyboard() ? new BelowOrAboveWidgetTooltipPositioner(pScreenRectangle) : new MenuTooltipPositioner(pScreenRectangle));
   }
}