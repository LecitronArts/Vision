package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;

public class BossHealthOverlay {
   private static final int BAR_WIDTH = 182;
   private static final int BAR_HEIGHT = 5;
   private static final ResourceLocation[] BAR_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/pink_background"), new ResourceLocation("boss_bar/blue_background"), new ResourceLocation("boss_bar/red_background"), new ResourceLocation("boss_bar/green_background"), new ResourceLocation("boss_bar/yellow_background"), new ResourceLocation("boss_bar/purple_background"), new ResourceLocation("boss_bar/white_background")};
   private static final ResourceLocation[] BAR_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/pink_progress"), new ResourceLocation("boss_bar/blue_progress"), new ResourceLocation("boss_bar/red_progress"), new ResourceLocation("boss_bar/green_progress"), new ResourceLocation("boss_bar/yellow_progress"), new ResourceLocation("boss_bar/purple_progress"), new ResourceLocation("boss_bar/white_progress")};
   private static final ResourceLocation[] OVERLAY_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/notched_6_background"), new ResourceLocation("boss_bar/notched_10_background"), new ResourceLocation("boss_bar/notched_12_background"), new ResourceLocation("boss_bar/notched_20_background")};
   private static final ResourceLocation[] OVERLAY_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/notched_6_progress"), new ResourceLocation("boss_bar/notched_10_progress"), new ResourceLocation("boss_bar/notched_12_progress"), new ResourceLocation("boss_bar/notched_20_progress")};
   private final Minecraft minecraft;
   final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

   public BossHealthOverlay(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void render(GuiGraphics pGuiGraphics) {
      if (!this.events.isEmpty()) {
         int i = pGuiGraphics.guiWidth();
         int j = 12;

         for(LerpingBossEvent lerpingbossevent : this.events.values()) {
            int k = i / 2 - 91;
            boolean flag = true;
            int l = 19;


            if (flag) {
               this.drawBar(pGuiGraphics, k, j, lerpingbossevent);
               Component component = lerpingbossevent.getName();
               int i1 = this.minecraft.font.width(component);
               int j1 = i / 2 - i1 / 2;
               int k1 = j - 9;
               int l1 = 16777215;
               if (Config.isCustomColors()) {
                  l1 = CustomColors.getBossTextColor(l1);
               }

               pGuiGraphics.drawString(this.minecraft.font, component, j1, k1, l1);
            }

            j += l;
            if (j >= pGuiGraphics.guiHeight() / 3) {
               break;
            }
         }
      }

   }

   private void drawBar(GuiGraphics pGuiGraphics, int pX, int pY, BossEvent pBossEvent) {
      this.drawBar(pGuiGraphics, pX, pY, pBossEvent, 182, BAR_BACKGROUND_SPRITES, OVERLAY_BACKGROUND_SPRITES);
      int i = Mth.lerpDiscrete(pBossEvent.getProgress(), 0, 182);
      if (i > 0) {
         this.drawBar(pGuiGraphics, pX, pY, pBossEvent, i, BAR_PROGRESS_SPRITES, OVERLAY_PROGRESS_SPRITES);
      }

   }

   private void drawBar(GuiGraphics pGuiGraphics, int pX, int pY, BossEvent pBossEvent, int pProgress, ResourceLocation[] pBarProgressSprites, ResourceLocation[] pOverlayProgressSprites) {
      pGuiGraphics.blitSprite(pBarProgressSprites[pBossEvent.getColor().ordinal()], 182, 5, 0, 0, pX, pY, pProgress, 5);
      if (pBossEvent.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
         RenderSystem.enableBlend();
         pGuiGraphics.blitSprite(pOverlayProgressSprites[pBossEvent.getOverlay().ordinal() - 1], 182, 5, 0, 0, pX, pY, pProgress, 5);
         RenderSystem.disableBlend();
      }

   }

   public void update(ClientboundBossEventPacket pPacket) {
      pPacket.dispatch(new ClientboundBossEventPacket.Handler() {
         public void add(UUID p_168824_, Component p_168825_, float p_168826_, BossEvent.BossBarColor p_168827_, BossEvent.BossBarOverlay p_168828_, boolean p_168829_, boolean p_168830_, boolean p_168831_) {
            BossHealthOverlay.this.events.put(p_168824_, new LerpingBossEvent(p_168824_, p_168825_, p_168826_, p_168827_, p_168828_, p_168829_, p_168830_, p_168831_));
         }

         public void remove(UUID p_168812_) {
            BossHealthOverlay.this.events.remove(p_168812_);
         }

         public void updateProgress(UUID p_168814_, float p_168815_) {
            BossHealthOverlay.this.events.get(p_168814_).setProgress(p_168815_);
         }

         public void updateName(UUID p_168821_, Component p_168822_) {
            BossHealthOverlay.this.events.get(p_168821_).setName(p_168822_);
         }

         public void updateStyle(UUID p_168817_, BossEvent.BossBarColor p_168818_, BossEvent.BossBarOverlay p_168819_) {
            LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168817_);
            lerpingbossevent.setColor(p_168818_);
            lerpingbossevent.setOverlay(p_168819_);
         }

         public void updateProperties(UUID p_168833_, boolean p_168834_, boolean p_168835_, boolean p_168836_) {
            LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168833_);
            lerpingbossevent.setDarkenScreen(p_168834_);
            lerpingbossevent.setPlayBossMusic(p_168835_);
            lerpingbossevent.setCreateWorldFog(p_168836_);
         }
      });
   }

   public void reset() {
      this.events.clear();
   }

   public boolean shouldPlayMusic() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldPlayBossMusic()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldDarkenScreen() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldDarkenScreen()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldCreateWorldFog() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldCreateWorldFog()) {
               return true;
            }
         }
      }

      return false;
   }

   public String getBossName() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            Component component = bossevent.getName();
            if (component != null) {
               ComponentContents componentcontents = component.getContents();
               if (componentcontents instanceof TranslatableContents) {
                  TranslatableContents translatablecontents = (TranslatableContents)componentcontents;
                  return translatablecontents.getKey();
               }
            }
         }
      }

      return null;
   }
}