package net.minecraft.client.gui.screens;

import java.util.function.Supplier;

import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.Difficulty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OptionsScreen extends Screen {
   private static final Component SKIN_CUSTOMIZATION = Component.translatable("options.skinCustomisation");
   private static final Component SOUNDS = Component.translatable("options.sounds");
   private static final Component VIDEO = Component.translatable("options.video");
   private static final Component CONTROLS = Component.translatable("options.controls");
   private static final Component LANGUAGE = Component.translatable("options.language");
   private static final Component CHAT = Component.translatable("options.chat");
   private static final Component RESOURCEPACK = Component.translatable("options.resourcepack");
   private static final Component ACCESSIBILITY = Component.translatable("options.accessibility");
   private static final Component TELEMETRY = Component.translatable("options.telemetry");
   private static final Component CREDITS_AND_ATTRIBUTION = Component.translatable("options.credits_and_attribution");
   private static final int COLUMNS = 2;
   private final Screen lastScreen;
   private final Options options;
   private CycleButton<Difficulty> difficultyButton;
   private LockIconButton lockButton;

   public OptionsScreen(Screen pLastScreen, Options pOptions) {
      super(Component.translatable("options.title"));
      this.lastScreen = pLastScreen;
      this.options = pOptions;
   }

   protected void init() {
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().paddingHorizontal(5).paddingBottom(4).alignHorizontallyCenter();
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
      gridlayout$rowhelper.addChild(this.options.fov().createButton(this.minecraft.options, 0, 0, 150));
      gridlayout$rowhelper.addChild(this.createOnlineButton());
      gridlayout$rowhelper.addChild(SpacerElement.height(26), 2);
      gridlayout$rowhelper.addChild(this.openScreenButton(SKIN_CUSTOMIZATION, () -> {
         return new SkinCustomizationScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(SOUNDS, () -> {
         return new SoundOptionsScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(VIDEO, () -> {
         return new VideoSettingsScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(CONTROLS, () -> {
         return new ControlsScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(LANGUAGE, () -> {
         return new LanguageSelectScreen(this, this.options, this.minecraft.getLanguageManager());
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(CHAT, () -> {
         return new ChatOptionsScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(RESOURCEPACK, () -> {
         return new PackSelectionScreen(this.minecraft.getResourcePackRepository(), this::applyPacks, this.minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title"));
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(ACCESSIBILITY, () -> {
         return new AccessibilityOptionsScreen(this, this.options);
      }));
      gridlayout$rowhelper.addChild(this.openScreenButton(TELEMETRY, () -> {
         return new TelemetryInfoScreen(this, this.options);
      }));
      if (VisualSettings.global().showSuperSecretSettings.isEnabled() && Minecraft.getInstance().player != null) {
         this.addRenderableWidget(Button.builder(Component.literal("Super Secret Settings..."), button -> Minecraft.getInstance().gameRenderer.cycleEffect()).bounds(this.width / 2 + 5, this.height / 6 + 18, 150, 20).build());
      }
      gridlayout$rowhelper.addChild(this.openScreenButton(CREDITS_AND_ATTRIBUTION, () -> {
         return new CreditsAndAttributionScreen(this);
      }));
      gridlayout$rowhelper.addChild(Button.builder(CommonComponents.GUI_DONE, (p_280809_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).width(200).build(), 2, gridlayout$rowhelper.newCellSettings().paddingTop(6));
      gridlayout.arrangeElements();
      FrameLayout.alignInRectangle(gridlayout, 0, this.height / 6 - 12, this.width, this.height, 0.5F, 0.0F);
      gridlayout.visitWidgets(this::addRenderableWidget);
   }

   private void applyPacks(PackRepository p_275714_) {
      this.options.updateResourcePacks(p_275714_);
      this.minecraft.setScreen(this);
   }

   private LayoutElement createOnlineButton() {
      if (this.minecraft.level != null && this.minecraft.hasSingleplayerServer()) {
         this.difficultyButton = createDifficultyButton(0, 0, "options.difficulty", this.minecraft);
         if (!this.minecraft.level.getLevelData().isHardcore()) {
            this.lockButton = new LockIconButton(0, 0, (p_280806_) -> {
               this.minecraft.setScreen(new ConfirmScreen(this::lockCallback, Component.translatable("difficulty.lock.title"), Component.translatable("difficulty.lock.question", this.minecraft.level.getLevelData().getDifficulty().getDisplayName())));
            });
            this.difficultyButton.setWidth(this.difficultyButton.getWidth() - this.lockButton.getWidth());
            this.lockButton.setLocked(this.minecraft.level.getLevelData().isDifficultyLocked());
            this.lockButton.active = !this.lockButton.isLocked();
            this.difficultyButton.active = !this.lockButton.isLocked();
            EqualSpacingLayout equalspacinglayout = new EqualSpacingLayout(150, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            equalspacinglayout.addChild(this.difficultyButton);
            equalspacinglayout.addChild(this.lockButton);
            return equalspacinglayout;
         } else {
            this.difficultyButton.active = false;
            return this.difficultyButton;
         }
      } else {
         return Button.builder(Component.translatable("options.online"), (p_280805_) -> {
            this.minecraft.setScreen(OnlineOptionsScreen.createOnlineOptionsScreen(this.minecraft, this, this.options));
         }).bounds(this.width / 2 + 5, this.height / 6 - 12 + 24, 150, 20).build();
      }
   }

   public static CycleButton<Difficulty> createDifficultyButton(int pX, int pY, String pTranslationKey, Minecraft pMinecraft) {
      return CycleButton.builder(Difficulty::getDisplayName).withValues(Difficulty.values()).withInitialValue(pMinecraft.level.getDifficulty()).create(pX, pY, 150, 20, Component.translatable(pTranslationKey), (p_296186_, p_296187_) -> {
         pMinecraft.getConnection().send(new ServerboundChangeDifficultyPacket(p_296187_));
      });
   }

   private void lockCallback(boolean p_96261_) {
      this.minecraft.setScreen(this);
      if (p_96261_ && this.minecraft.level != null) {
         this.minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
         this.lockButton.setLocked(true);
         this.lockButton.active = false;
         this.difficultyButton.active = false;
      }

   }

   public void onClose() {
      minecraft.setScreen(lastScreen);
   }

   public void removed() {
      this.options.save();
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
   }

   private Button openScreenButton(Component pText, Supplier<Screen> pScreenSupplier) {
      return Button.builder(pText, (p_280808_) -> {
         this.minecraft.setScreen(pScreenSupplier.get());
      }).build();
   }
}