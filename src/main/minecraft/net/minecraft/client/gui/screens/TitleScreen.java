package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import net.burningtnt.accountsx.AccountsX;
import net.burningtnt.accountsx.config.AccountManager;
import net.burningtnt.accountsx.gui.AccountScreen;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;

public class TitleScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEMO_LEVEL_ID = "Demo_World";
   public static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
   public static final CubeMap CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
   private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
   @Nullable
   private SplashRenderer splash;
   private Button resetDemoButton;
   @Nullable
   private RealmsNotificationsScreen realmsNotificationsScreen;
   private final PanoramaRenderer panorama = new PanoramaRenderer(CUBE_MAP);
   private final boolean fading;
   private long fadeInStart;
   @Nullable
   private TitleScreen.WarningLabel warningLabel;
   private final LogoRenderer logoRenderer;
   private Screen modUpdateNotification;


   public TitleScreen() {
      this(false);
   }

   public TitleScreen(boolean pFading) {
      this(pFading, (LogoRenderer)null);
   }

   public TitleScreen(boolean pFading, @Nullable LogoRenderer pLogoRenderer) {
      super(Component.translatable("narrator.screen.title"));
      this.fading = pFading;
      this.logoRenderer = Objects.requireNonNullElseGet(pLogoRenderer, () -> {
         return new LogoRenderer(false);
      });
   }

   private boolean realmsNotificationsEnabled() {
      return this.realmsNotificationsScreen != null;
   }

   public void tick() {
      if (this.realmsNotificationsEnabled()) {
         this.realmsNotificationsScreen.tick();
      }

      this.minecraft.getRealms32BitWarningStatus().showRealms32BitWarningIfNeeded(this);
   }

   public static CompletableFuture<Void> preloadResources(TextureManager pTexMngr, Executor pBackgroundExecutor) {
      return CompletableFuture.allOf(pTexMngr.preload(LogoRenderer.MINECRAFT_LOGO, pBackgroundExecutor), pTexMngr.preload(LogoRenderer.MINECRAFT_EDITION, pBackgroundExecutor), pTexMngr.preload(PANORAMA_OVERLAY, pBackgroundExecutor), CUBE_MAP.preload(pTexMngr, pBackgroundExecutor));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void init() {
      if (this.splash == null) {
         this.splash = this.minecraft.getSplashManager().getSplash();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(new Date());
         int i = calendar.get(5);
         int j = calendar.get(2) + 1;
         if (i == 8 && j == 4) {
            this.splash = new SplashRenderer("Happy birthday, OptiFine!");
         }

         if (i == 14 && j == 8) {
            this.splash = new SplashRenderer("Happy birthday, sp614x!");
         }
      }

      int l = this.font.width(COPYRIGHT_TEXT);
      int i1 = this.width - l - 2;
      int j1 = 24;
      int k = this.height / 4 + 48;
      Button button = null;
      if (this.minecraft.isDemo()) {
         this.createDemoMenuOptions(k, 24);
      } else {
         this.createNormalMenuOptions(k, 24);
         if (Reflector.ModListScreen_Constructor.exists()) {
            button = ReflectorForge.makeButtonMods(this, k, 24);
            this.addRenderableWidget(button);
         }
      }

      SpriteIconButton spriteiconbutton = this.addRenderableWidget(CommonButtons.language(20, (p_279793_1_) -> {
         this.minecraft.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));
      }, true));
      spriteiconbutton.setPosition(this.width / 2 - 124, k + 72 + 12);
      this.addRenderableWidget(Button.builder(Component.translatable("menu.options"), (p_279800_1_) -> {
         this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
      }).bounds(this.width / 2 - 100, k + 72 + 12, 98, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (p_279794_1_) -> {
         this.minecraft.stop();
      }).bounds(this.width / 2 + 2, k + 72 + 12, 98, 20).build());
      SpriteIconButton spriteiconbutton1 = this.addRenderableWidget(CommonButtons.accessibility(20, (p_279798_1_) -> {
         this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options));
      }, true));
      spriteiconbutton1.setPosition(this.width / 2 + 104, k + 72 + 12);
      this.addRenderableWidget(new PlainTextButton(i1, this.height - 10, l, 10, COPYRIGHT_TEXT, (p_279797_1_) -> {
         this.minecraft.setScreen(new CreditsAndAttributionScreen(this));
      }, this.font));
      if (this.realmsNotificationsScreen == null) {
         this.realmsNotificationsScreen = new RealmsNotificationsScreen();
      }

      if (this.realmsNotificationsEnabled()) {
         this.realmsNotificationsScreen.init(this.minecraft, this.width, this.height);
      }

      if (!this.minecraft.is64Bit()) {
         this.warningLabel = new TitleScreen.WarningLabel(this.font, MultiLineLabel.create(this.font, Component.translatable("title.32bit.deprecation"), 350, 2), this.width / 2, k - 24);
      }

      if (Reflector.TitleScreenModUpdateIndicator_init.exists()) {
         this.modUpdateNotification = (Screen)Reflector.call(Reflector.TitleScreenModUpdateIndicator_init, this, button);
      }

   }

   private void createNormalMenuOptions(int pY, int pRowHeight) {
      this.addRenderableWidget(Button.builder(Component.translatable("menu.singleplayer"), (p_279795_1_) -> {
         this.minecraft.setScreen(new SelectWorldScreen(this));
      }).bounds(this.width / 2 - 100, pY, 200, 20).build());
      Component component = this.getMultiplayerDisabledReason();
      boolean flag = component == null;
      Tooltip tooltip = component != null ? Tooltip.create(component) : null;
      (this.addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), (p_279796_1_) -> {
         Screen screen = (Screen)(this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this));
         this.minecraft.setScreen(screen);
      }).bounds(this.width / 2 - 100, pY + pRowHeight * 1, 200, 20).tooltip(tooltip).build())).active = flag;
      boolean flag1 = Reflector.ModListScreen_Constructor.exists();
      int i = flag1 ? this.width / 2 + 2 : this.width / 2 - 100;
      int j = flag1 ? 98 : 200;
      this.addRenderableWidget(Button.builder(Component.translatable("menu.online"), (p_210871_1_) ->
              this.realmsButtonClicked()).
              bounds(i, pY + pRowHeight * 2, j, 20)
              .tooltip(tooltip).build()).
              active = flag;
      this.addRenderableWidget(Button.builder(Component.literal("Alt"), (p_210871_1_) ->
              this.minecraft.setScreen(new AccountScreen(this))).
              bounds(this.width / 2 + 104, pY + pRowHeight * 2, 20, 20)
              .tooltip(tooltip).build()).
              active = flag;
   }

   @Nullable
   private Component getMultiplayerDisabledReason() {
      if (this.minecraft.allowsMultiplayer()) {
         return null;
      } else if (this.minecraft.isNameBanned()) {
         return Component.translatable("title.multiplayer.disabled.banned.name");
      } else {
         BanDetails bandetails = this.minecraft.multiplayerBan();
         if (bandetails != null) {
            return bandetails.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary") : Component.translatable("title.multiplayer.disabled.banned.permanent");
         } else {
            return Component.translatable("title.multiplayer.disabled");
         }
      }
   }

   private void createDemoMenuOptions(int pY, int pRowHeight) {
      boolean flag = this.checkDemoWorldPresence();
      this.addRenderableWidget(Button.builder(Component.translatable("menu.playdemo"), (p_303996_2_) -> {
         if (flag) {
            this.minecraft.createWorldOpenFlows().checkForBackupAndLoad("Demo_World", () -> {
               this.minecraft.setScreen(this);
            });
         } else {
            this.minecraft.createWorldOpenFlows().createFreshLevel("Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this);
         }

      }).bounds(this.width / 2 - 100, pY, 200, 20).build());
      this.resetDemoButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.resetdemo"), (p_303995_1_) -> {
         LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();

         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess("Demo_World")) {
            if (levelstoragesource$levelstorageaccess.hasWorldData()) {
               this.minecraft.setScreen(new ConfirmScreen(this::confirmDemo, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_SETTINGS.levelName()), Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
            }
         } catch (IOException ioexception1) {
            SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to access demo world", (Throwable)ioexception1);
         }

      }).bounds(this.width / 2 - 100, pY + pRowHeight * 1, 200, 20).build());
      this.resetDemoButton.active = flag;
   }

   private boolean checkDemoWorldPresence() {
      try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess("Demo_World")) {
         return levelstoragesource$levelstorageaccess.hasWorldData();
      } catch (IOException ioexception1) {
         SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
         LOGGER.warn("Failed to read demo world data", (Throwable)ioexception1);
         return false;
      }
   }

   private void realmsButtonClicked() {
      this.minecraft.setScreen(new RealmsMainScreen(this));
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.fadeInStart == 0L && this.fading) {
         this.fadeInStart = Util.getMillis();
      }

      float f = this.fading ? (float)(Util.getMillis() - this.fadeInStart) / 1000.0F : 1.0F;
      GlStateManager._disableDepthTest();
      this.panorama.render(pPartialTick, Mth.clamp(f, 0.0F, 1.0F));
      RenderSystem.enableBlend();
      pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.fading ? (float)Mth.ceil(Mth.clamp(f, 0.0F, 1.0F)) : 1.0F);
      pGuiGraphics.blit(PANORAMA_OVERLAY, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
      pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f1 = this.fading ? Mth.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
      this.logoRenderer.renderLogo(pGuiGraphics, this.width, f1);
      int i = Mth.ceil(f1 * 255.0F) << 24;
      if ((i & -67108864) != 0) {
         if (this.warningLabel != null) {
            this.warningLabel.render(pGuiGraphics, i);
         }

         if (Reflector.ForgeHooksClient_renderMainMenu.exists()) {
            Reflector.callVoid(Reflector.ForgeHooksClient_renderMainMenu, this, pGuiGraphics, this.font, this.width, this.height, i);
         }

         if (this.splash != null && !this.minecraft.options.hideSplashTexts().get()) {
            this.splash.render(pGuiGraphics, this.width, this.font, i);
         }

         String s = "Minecraft " + SharedConstants.getCurrentVersion().getName();
         if (this.minecraft.isDemo()) {
            s = s + " Demo";
         } else {
            s = s + ("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType());
         }

         if (Minecraft.checkModStatus().shouldReportAsModified()) {
            s = s + I18n.get("menu.modded");
         }

         pGuiGraphics.drawString(this.font, s, 2, this.height - 10, 16777215 | i);

         for(GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget) {
               ((AbstractWidget)guieventlistener).setAlpha(f1);
            }
         }

         if ((i & -67108864) != 0) {
            pGuiGraphics.drawCenteredString(this.font, AccountManager.getCurrentAccountInfoText(), this.width / 2, 15, 0xFFFFFF | i);
         }

         super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         if (this.realmsNotificationsEnabled() && f1 >= 1.0F) {
            RenderSystem.enableDepthTest();
            this.realmsNotificationsScreen.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         }
      }

      if (this.modUpdateNotification != null && f1 >= 1.0F) {
         this.modUpdateNotification.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }

   }

   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
         return true;
      } else {
         return this.realmsNotificationsEnabled() && this.realmsNotificationsScreen.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   public void removed() {
      if (this.realmsNotificationsScreen != null) {
         this.realmsNotificationsScreen.removed();
      }

   }

   public void added() {
      super.added();
      if (this.realmsNotificationsScreen != null) {
         this.realmsNotificationsScreen.added();
      }

   }

   private void confirmDemo(boolean p_96778_) {
      if (p_96778_) {
         try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess("Demo_World")) {
            levelstoragesource$levelstorageaccess.deleteLevel();
         } catch (IOException ioexception1) {
            SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to delete demo world", (Throwable)ioexception1);
         }
      }

      this.minecraft.setScreen(this);
   }

   static record WarningLabel(Font font, MultiLineLabel label, int x, int y) {
      public void render(GuiGraphics pGuiGraphics, int pColor) {
         this.label.renderBackgroundCentered(pGuiGraphics, this.x, this.y, 9, 2, 2097152 | Math.min(pColor, 1426063360));
         this.label.renderCentered(pGuiGraphics, this.x, this.y, 9, 16777215 | pColor);
      }

      public Font font() {
         return this.font;
      }

      public MultiLineLabel label() {
         return this.label;
      }

      public int x() {
         return this.x;
      }

      public int y() {
         return this.y;
      }
   }
}
