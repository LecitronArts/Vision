package net.minecraft.client.gui.screens;

import java.util.function.BooleanSupplier;

import com.viaversion.viaversion.api.connection.UserConnection;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import de.florianmichael.viafabricplus.util.ChatUtil;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.storage.ClassicProgressStorage;

public class ReceivingLevelScreen extends Screen {
   private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
   private static final long CHUNK_LOADING_START_WAIT_LIMIT_MS = 30000L;
   private final long createdAt;
   private final BooleanSupplier levelReceived;
   private CustomLoadingScreen customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();

   public ReceivingLevelScreen(BooleanSupplier pLevelReceived) {
      super(GameNarrator.NO_TITLE);
      this.levelReceived = pLevelReceived;
      this.createdAt = System.currentTimeMillis();
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected boolean shouldNarrateNavigation() {
      return false;
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, DOWNLOADING_TERRAIN_TEXT, this.width / 2, this.height / 2 - 50, 16777215);
      if (GeneralSettings.global().showClassicLoadingProgressInConnectScreen.getValue()) {
         // Check if ViaVersion is translating
         final UserConnection connection = ProtocolTranslator.getPlayNetworkUserConnection();
         if (connection == null) {
            return;
         }

         // Check if the client is connecting to a classic server
         final ClassicProgressStorage classicProgressStorage = connection.get(ClassicProgressStorage.class);
         if (classicProgressStorage == null) {
            return;
         }

         // Draw the classic loading progress
         pGuiGraphics.drawCenteredString(
                 minecraft.font,
                 ChatUtil.prefixText(classicProgressStorage.status),
                 width / 2,
                 height / 2 - 30,
                 -1
         );
      }
   }

   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.customLoadingScreen != null) {
         this.customLoadingScreen.drawBackground(this.width, this.height);
      } else {
         this.renderDirtBackground(pGuiGraphics);
      }

   }

   public void tick() {
      if (this.levelReceived.getAsBoolean() || System.currentTimeMillis() > this.createdAt + 30000L) {
         this.onClose();
      }

   }

   public void onClose() {
      this.minecraft.getNarrator().sayNow(Component.translatable("narrator.ready_to_play"));
      super.onClose();
   }

   public boolean isPauseScreen() {
      return false;
   }
}