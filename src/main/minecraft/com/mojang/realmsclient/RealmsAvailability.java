package com.mojang.realmsclient;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsAvailability {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private static CompletableFuture<RealmsAvailability.Result> future;

   public static CompletableFuture<RealmsAvailability.Result> get() {
      if (future == null || shouldRefresh(future)) {
         future = check();
      }

      return future;
   }

   private static boolean shouldRefresh(CompletableFuture<RealmsAvailability.Result> pFuture) {
      RealmsAvailability.Result realmsavailability$result = pFuture.getNow((RealmsAvailability.Result)null);
      return realmsavailability$result != null && realmsavailability$result.exception() != null;
   }

   private static CompletableFuture<RealmsAvailability.Result> check() {
      return CompletableFuture.supplyAsync(() -> {
         RealmsClient realmsclient = RealmsClient.create();

         try {
            if (realmsclient.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
               return new RealmsAvailability.Result(RealmsAvailability.Type.INCOMPATIBLE_CLIENT);
            } else {
               return !realmsclient.hasParentalConsent() ? new RealmsAvailability.Result(RealmsAvailability.Type.NEEDS_PARENTAL_CONSENT) : new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS);
            }
         } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't connect to realms", (Throwable)realmsserviceexception);
            return realmsserviceexception.realmsError.errorCode() == 401 ? new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR) : new RealmsAvailability.Result(realmsserviceexception);
         }
      }, Util.ioPool());
   }

   @OnlyIn(Dist.CLIENT)
   public static record Result(RealmsAvailability.Type type, @Nullable RealmsServiceException exception) {
      public Result(RealmsAvailability.Type pType) {
         this(pType, (RealmsServiceException)null);
      }

      public Result(RealmsServiceException pException) {
         this(RealmsAvailability.Type.UNEXPECTED_ERROR, pException);
      }

      @Nullable
      public Screen createErrorScreen(Screen pLastScreen) {
         Object object;
         switch (this.type) {
            case SUCCESS:
               object = null;
               break;
            case INCOMPATIBLE_CLIENT:
               object = new RealmsClientOutdatedScreen(pLastScreen);
               break;
            case NEEDS_PARENTAL_CONSENT:
               object = new RealmsParentalConsentScreen(pLastScreen);
               break;
            case AUTHENTICATION_ERROR:
               object = new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), pLastScreen);
               break;
            case UNEXPECTED_ERROR:
               object = new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), pLastScreen);
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return (Screen)object;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      SUCCESS,
      INCOMPATIBLE_CLIENT,
      NEEDS_PARENTAL_CONSENT,
      AUTHENTICATION_ERROR,
      UNEXPECTED_ERROR;
   }
}