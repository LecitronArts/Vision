package net.minecraft.client.gui.screens;

import java.util.function.BooleanSupplier;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import de.florianmichael.viafabricplus.util.ChatUtil;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.storage.ClassicProgressStorage;
import org.spongepowered.asm.mixin.Unique;

public class ReceivingLevelScreen extends Screen {
   private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
   private static final long CHUNK_LOADING_START_WAIT_LIMIT_MS = 30000L;
   private final long createdAt;
   private final BooleanSupplier levelReceived;
   private CustomLoadingScreen customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();
   @Unique
   private int viaFabricPlus$tickCounter;

   @Unique
   private boolean viaFabricPlus$ready;

   @Unique
   private boolean viaFabricPlus$closeOnNextTick = false;
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
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {


         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            if (this.viaFabricPlus$ready) {
               this.onClose();
            }

            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_1)) {
               this.viaFabricPlus$tickCounter++;
               if (this.viaFabricPlus$tickCounter % 20 == 0) {
                  this.minecraft.getConnection().send(new ServerboundKeepAlivePacket(0));
               }
            }
         } else {
            if (System.currentTimeMillis() > this.createdAt + 30000L) {
               this.onClose();
            } else {
               if (this.viaFabricPlus$closeOnNextTick) {
                  if (this.minecraft.player == null) return;

                  final BlockPos blockPos = this.minecraft.player.blockPosition();
                  final boolean isOutOfHeightLimit = this.minecraft.level != null && this.minecraft.level.isOutsideBuildHeight(blockPos.getY());
                  if (isOutOfHeightLimit || this.minecraft.levelRenderer.isSectionCompiled(blockPos) || this.minecraft.player.isSpectator() || !this.minecraft.player.isAlive()) {
                     this.onClose();
                  }
               } else {
                  if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1)) {
                     this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready || System.currentTimeMillis() > this.createdAt + 2000;
                  } else {
                     this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready;
                  }
               }
            }
         }
         return;
      }
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
   public void viaFabricPlus$setReady() {
      this.viaFabricPlus$ready = true;
   }
}