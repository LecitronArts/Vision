package net.minecraft.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;

public class StatsComponent extends JComponent {
   private static final DecimalFormat DECIMAL_FORMAT = Util.make(new DecimalFormat("########0.000"), (p_139968_) -> {
      p_139968_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
   });
   private final int[] values = new int[256];
   private int vp;
   private final String[] msgs = new String[11];
   private final MinecraftServer server;
   private final Timer timer;

   public StatsComponent(MinecraftServer pServer) {
      this.server = pServer;
      this.setPreferredSize(new Dimension(456, 246));
      this.setMinimumSize(new Dimension(456, 246));
      this.setMaximumSize(new Dimension(456, 246));
      this.timer = new Timer(500, (p_139966_) -> {
         this.tick();
      });
      this.timer.start();
      this.setBackground(Color.BLACK);
   }

   private void tick() {
      long i = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      this.msgs[0] = "Memory use: " + i / 1024L / 1024L + " mb (" + Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory() + "% free)";
      this.msgs[1] = "Avg tick: " + DECIMAL_FORMAT.format((double)this.server.getAverageTickTimeNanos() / (double)TimeUtil.NANOSECONDS_PER_MILLISECOND) + " ms";
      this.values[this.vp++ & 255] = (int)(i * 100L / Runtime.getRuntime().maxMemory());
      this.repaint();
   }

   public void paint(Graphics pGraphics) {
      pGraphics.setColor(new Color(16777215));
      pGraphics.fillRect(0, 0, 456, 246);

      for(int i = 0; i < 256; ++i) {
         int j = this.values[i + this.vp & 255];
         pGraphics.setColor(new Color(j + 28 << 16));
         pGraphics.fillRect(i, 100 - j, 1, j);
      }

      pGraphics.setColor(Color.BLACK);

      for(int k = 0; k < this.msgs.length; ++k) {
         String s = this.msgs[k];
         if (s != null) {
            pGraphics.drawString(s, 32, 116 + k * 16);
         }
      }

   }

   public void close() {
      this.timer.stop();
   }
}