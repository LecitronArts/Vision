package net.minecraft.client;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.Realms32bitWarningScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Realms32BitWarningStatus {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private CompletableFuture<Boolean> subscriptionCheck;
   private boolean warningScreenShown;

   public Realms32BitWarningStatus(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void showRealms32BitWarningIfNeeded(Screen pLastScreen) {
      if (!this.minecraft.is64Bit() && !this.minecraft.options.skipRealms32bitWarning && !this.warningScreenShown && this.checkForRealmsSubscription()) {
         this.minecraft.setScreen(new Realms32bitWarningScreen(pLastScreen));
         this.warningScreenShown = true;
      }

   }

   private Boolean checkForRealmsSubscription() {
      if (this.subscriptionCheck == null) {
         this.subscriptionCheck = CompletableFuture.supplyAsync(this::hasRealmsSubscription, Util.backgroundExecutor());
      }

      try {
         return this.subscriptionCheck.getNow(false);
      } catch (CompletionException completionexception) {
         LOGGER.warn("Failed to retrieve realms subscriptions", (Throwable)completionexception);
         this.warningScreenShown = true;
         return false;
      }
   }

   private boolean hasRealmsSubscription() {
      try {
         return RealmsClient.create(this.minecraft).listWorlds().servers.stream().anyMatch((p_296175_) -> {
            return !p_296175_.expired && this.minecraft.isLocalPlayer(p_296175_.ownerUUID);
         });
      } catch (RealmsServiceException realmsserviceexception) {
         return false;
      }
   }
}