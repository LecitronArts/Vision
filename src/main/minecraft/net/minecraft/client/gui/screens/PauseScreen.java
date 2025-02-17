package net.minecraft.client.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PauseScreen extends Screen {
   private static final ResourceLocation DRAFT_REPORT_SPRITE = new ResourceLocation("icon/draft_report");
   private static final int COLUMNS = 2;
   private static final int MENU_PADDING_TOP = 50;
   private static final int BUTTON_PADDING = 4;
   private static final int BUTTON_WIDTH_FULL = 204;
   private static final int BUTTON_WIDTH_HALF = 98;
   private static final Component RETURN_TO_GAME = Component.translatable("menu.returnToGame");
   private static final Component ADVANCEMENTS = Component.translatable("gui.advancements");
   private static final Component STATS = Component.translatable("gui.stats");
   private static final Component SEND_FEEDBACK = Component.translatable("menu.sendFeedback");
   private static final Component REPORT_BUGS = Component.translatable("menu.reportBugs");
   private static final Component OPTIONS = Component.translatable("menu.options");
   private static final Component SHARE_TO_LAN = Component.translatable("menu.shareToLan");
   private static final Component PLAYER_REPORTING = Component.translatable("menu.playerReporting");
   private static final Component RETURN_TO_MENU = Component.translatable("menu.returnToMenu");
   private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
   private static final Component GAME = Component.translatable("menu.game");
   private static final Component PAUSED = Component.translatable("menu.paused");
   private final boolean showPauseMenu;
   @Nullable
   private Button disconnectButton;

   public PauseScreen(boolean pShowPauseMenu) {
      super(pShowPauseMenu ? GAME : PAUSED);
      this.showPauseMenu = pShowPauseMenu;
   }

   public boolean showsPauseMenu() {
      return this.showPauseMenu;
   }

   protected void init() {
      if (this.showPauseMenu) {
         this.createPauseMenu();
      }

      this.addRenderableWidget(new StringWidget(0, this.showPauseMenu ? 40 : 10, this.width, 9, this.title, this.font));
   }

   private void createPauseMenu() {
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().padding(4, 4, 4, 0);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
      gridlayout$rowhelper.addChild(Button.builder(RETURN_TO_GAME, (p_280814_) -> {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
      }).width(204).build(), 2, gridlayout.newCellSettings().paddingTop(50));
      gridlayout$rowhelper.addChild(this.openScreenButton(ADVANCEMENTS, () -> {
         return new AdvancementsScreen(this.minecraft.player.connection.getAdvancements());
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(STATS, () -> {
         return new StatsScreen(this, this.minecraft.player.getStats());
      }));
      gridlayout$rowhelper.addChild(this.openLinkButton(SEND_FEEDBACK, SharedConstants.getCurrentVersion().isStable() ? "https://aka.ms/javafeedback?ref=game" : "https://aka.ms/snapshotfeedback?ref=game"));
      (gridlayout$rowhelper.addChild(this.openLinkButton(REPORT_BUGS, "https://aka.ms/snapshotbugs?ref=game"))).active = !SharedConstants.getCurrentVersion().getDataVersion().isSideSeries();
      gridlayout$rowhelper.addChild(this.openScreenButton(OPTIONS, () -> {
         return new OptionsScreen(this, this.minecraft.options);
      }));
      if (this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished()) {
         gridlayout$rowhelper.addChild(this.openScreenButton(SHARE_TO_LAN, () -> {
            return new ShareToLanScreen(this);
         }));
      } else {
         gridlayout$rowhelper.addChild(this.openScreenButton(PLAYER_REPORTING, SocialInteractionsScreen::new));
      }

      Component component = this.minecraft.isLocalServer() ? RETURN_TO_MENU : CommonComponents.GUI_DISCONNECT;
      this.disconnectButton = gridlayout$rowhelper.addChild(Button.builder(component, (p_280815_) -> {
         p_280815_.active = false;
         this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::onDisconnect, true);
      }).width(204).build(), 2);
      gridlayout.arrangeElements();
      FrameLayout.alignInRectangle(gridlayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
      gridlayout.visitWidgets(this::addRenderableWidget);
   }

   private void onDisconnect() {
      boolean flag = this.minecraft.isLocalServer();
      ServerData serverdata = this.minecraft.getCurrentServer();
      this.minecraft.level.disconnect();
      if (flag) {
         this.minecraft.disconnect(new GenericDirtMessageScreen(SAVING_LEVEL));
      } else {
         this.minecraft.disconnect();
      }

      TitleScreen titlescreen = new TitleScreen();
      if (flag) {
         this.minecraft.setScreen(titlescreen);
      } else if (serverdata != null && serverdata.isRealm()) {
         this.minecraft.setScreen(new RealmsMainScreen(titlescreen));
      } else {
         this.minecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
      }

   }

   public void tick() {
      super.tick();
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (this.showPauseMenu && this.minecraft != null && this.minecraft.getReportingContext().hasDraftReport() && this.disconnectButton != null) {
         pGuiGraphics.blitSprite(DRAFT_REPORT_SPRITE, this.disconnectButton.getX() + this.disconnectButton.getWidth() - 17, this.disconnectButton.getY() + 3, 15, 15);
      }

   }

   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.showPauseMenu) {
         super.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }

   }

   private Button openScreenButton(Component pMessage, Supplier<Screen> pScreenSupplier) {
      return Button.builder(pMessage, (p_280817_) -> {
         this.minecraft.setScreen(pScreenSupplier.get());
      }).width(98).build();
   }

   private Button openLinkButton(Component pMessage, String pLinkUri) {
      return Button.builder(pMessage, ConfirmLinkScreen.confirmLink(this, pLinkUri)).width(98).build();
   }
}