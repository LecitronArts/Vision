package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectFileToUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Component TITLE = Component.translatable("mco.upload.select.world.title");
   private static final Component UNABLE_TO_LOAD_WORLD = Component.translatable("selectWorld.unable_to_load");
   static final Component WORLD_TEXT = Component.translatable("selectWorld.world");
   static final Component HARDCORE_TEXT = Component.translatable("mco.upload.hardcore").withColor(-65536);
   static final Component CHEATS_TEXT = Component.translatable("selectWorld.cheats");
   private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
   private final RealmsResetWorldScreen lastScreen;
   private final long worldId;
   private final int slotId;
   Button uploadButton;
   List<LevelSummary> levelList = Lists.newArrayList();
   int selectedWorld = -1;
   RealmsSelectFileToUploadScreen.WorldSelectionList worldSelectionList;

   public RealmsSelectFileToUploadScreen(long pWorldId, int pSlotId, RealmsResetWorldScreen pLastScreen) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.worldId = pWorldId;
      this.slotId = pSlotId;
   }

   private void loadLevelList() throws Exception {
      LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
      this.levelList = this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).join().stream().filter((p_193517_) -> {
         return !p_193517_.requiresManualConversion() && !p_193517_.isLocked();
      }).collect(Collectors.toList());

      for(LevelSummary levelsummary : this.levelList) {
         this.worldSelectionList.addEntry(levelsummary);
      }

   }

   public void init() {
      this.worldSelectionList = this.addRenderableWidget(new RealmsSelectFileToUploadScreen.WorldSelectionList());

      try {
         this.loadLevelList();
      } catch (Exception exception) {
         LOGGER.error("Couldn't load level list", (Throwable)exception);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(UNABLE_TO_LOAD_WORLD, Component.nullToEmpty(exception.getMessage()), this.lastScreen));
         return;
      }

      this.uploadButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.upload.button.name"), (p_231307_) -> {
         this.upload();
      }).bounds(this.width / 2 - 154, this.height - 32, 153, 20).build());
      this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_280747_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).bounds(this.width / 2 + 6, this.height - 32, 153, 20).build());
      this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.subtitle"), this.width / 2, row(-1), -6250336));
      if (this.levelList.isEmpty()) {
         this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, -1));
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   private void upload() {
      if (this.selectedWorld != -1 && !this.levelList.get(this.selectedWorld).isHardcore()) {
         LevelSummary levelsummary = this.levelList.get(this.selectedWorld);
         this.minecraft.setScreen(new RealmsUploadScreen(this.worldId, this.slotId, this.lastScreen, levelsummary));
      }

   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 13, -1);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   static Component gameModeName(LevelSummary pLevelSummary) {
      return pLevelSummary.getGameMode().getLongDisplayName();
   }

   static String formatLastPlayed(LevelSummary pLevelSummary) {
      return DATE_FORMAT.format(new Date(pLevelSummary.getLastPlayed()));
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsSelectFileToUploadScreen.Entry> {
      private final LevelSummary levelSummary;
      private final String name;
      private final Component id;
      private final Component info;

      public Entry(LevelSummary pLevelSummary) {
         this.levelSummary = pLevelSummary;
         this.name = pLevelSummary.getLevelName();
         this.id = Component.translatable("mco.upload.entry.id", pLevelSummary.getLevelId(), RealmsSelectFileToUploadScreen.formatLastPlayed(pLevelSummary));
         Component component;
         if (pLevelSummary.isHardcore()) {
            component = RealmsSelectFileToUploadScreen.HARDCORE_TEXT;
         } else {
            component = RealmsSelectFileToUploadScreen.gameModeName(pLevelSummary);
         }

         if (pLevelSummary.hasCheats()) {
            component = Component.translatable("mco.upload.entry.cheats", component.getString(), RealmsSelectFileToUploadScreen.CHEATS_TEXT);
         }

         this.info = component;
      }

      public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
         this.renderItem(pGuiGraphics, pIndex, pLeft, pTop);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         RealmsSelectFileToUploadScreen.this.worldSelectionList.selectItem(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.levelSummary));
         return true;
      }

      protected void renderItem(GuiGraphics pGuiGraphics, int pIndex, int pX, int pY) {
         String s;
         if (this.name.isEmpty()) {
            s = RealmsSelectFileToUploadScreen.WORLD_TEXT + " " + (pIndex + 1);
         } else {
            s = this.name;
         }

         pGuiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, s, pX + 2, pY + 1, 16777215, false);
         pGuiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, this.id, pX + 2, pY + 12, -8355712, false);
         pGuiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, this.info, pX + 2, pY + 12 + 10, -8355712, false);
      }

      public Component getNarration() {
         Component component = CommonComponents.joinLines(Component.literal(this.levelSummary.getLevelName()), Component.literal(RealmsSelectFileToUploadScreen.formatLastPlayed(this.levelSummary)), RealmsSelectFileToUploadScreen.gameModeName(this.levelSummary));
         return Component.translatable("narrator.select", component);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class WorldSelectionList extends RealmsObjectSelectionList<RealmsSelectFileToUploadScreen.Entry> {
      public WorldSelectionList() {
         super(RealmsSelectFileToUploadScreen.this.width, RealmsSelectFileToUploadScreen.this.height - 40 - RealmsSelectFileToUploadScreen.row(0), RealmsSelectFileToUploadScreen.row(0), 36);
      }

      public void addEntry(LevelSummary pLevelSummary) {
         this.addEntry(RealmsSelectFileToUploadScreen.this.new Entry(pLevelSummary));
      }

      public int getMaxPosition() {
         return RealmsSelectFileToUploadScreen.this.levelList.size() * 36;
      }

      public void setSelected(@Nullable RealmsSelectFileToUploadScreen.Entry pSelected) {
         super.setSelected(pSelected);
         RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(pSelected);
         RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0 && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getItemCount() && !RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld).isHardcore();
      }
   }
}