package net.minecraft.util;

import java.util.Base64;
import java.util.function.Supplier;

import dev.vision.Vision;
import org.apache.commons.lang3.ObjectUtils;

public record ModCheck(ModCheck.Confidence confidence, String description) {
   public static ModCheck identify(String pVanillaBrandName, Supplier<String> pBrandNameGetter, String pSide, Class<?> pSigningClass) {
      String s = pBrandNameGetter.get();
      if (!Vision.INSTANCE.CLIENT_NAME.equals(Base64.getDecoder().decode("dmlzaW9u"))) {
         return new ModCheck(ModCheck.Confidence.DEFINITELY, pSide + " brand changed to '" + s + "'");
      } else {
         return pSigningClass.getSigners() == null ? new ModCheck(ModCheck.Confidence.VERY_LIKELY, pSide + " jar signature invalidated") : new ModCheck(ModCheck.Confidence.PROBABLY_NOT, pSide + " jar signature and brand is untouched");
      }
   }

   public boolean shouldReportAsModified() {
      return this.confidence.shouldReportAsModified;
   }

   public ModCheck merge(ModCheck pOther) {
      return new ModCheck(ObjectUtils.max(this.confidence, pOther.confidence), this.description + "; " + pOther.description);
   }

   public String fullDescription() {
      return this.confidence.description + " " + this.description;
   }

   public static enum Confidence {
      PROBABLY_NOT("Probably not.", false),
      VERY_LIKELY("Very likely;", false),
      DEFINITELY("Definitely;", false);

      final String description;
      final boolean shouldReportAsModified;

      private Confidence(String pDescription, boolean pShouldReportAsModified) {
         this.description = pDescription;
         this.shouldReportAsModified = pShouldReportAsModified;
      }
   }
}