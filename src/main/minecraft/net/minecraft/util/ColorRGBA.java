package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public record ColorRGBA(int rgba) {
   private static final String CUSTOM_COLOR_PREFIX = "#";
   public static final Codec<ColorRGBA> CODEC = Codec.STRING.comapFlatMap((p_311033_) -> {
      if (!p_311033_.startsWith("#")) {
         return DataResult.error(() -> {
            return "Not a color code: " + p_311033_;
         });
      } else {
         try {
            int i = (int)Long.parseLong(p_311033_.substring(1), 16);
            return DataResult.success(new ColorRGBA(i));
         } catch (NumberFormatException numberformatexception) {
            return DataResult.error(() -> {
               return "Exception parsing color code: " + numberformatexception.getMessage();
            });
         }
      }
   }, ColorRGBA::formatValue);

   private String formatValue() {
      return String.format(Locale.ROOT, "#%08X", this.rgba);
   }

   public String toString() {
      return this.formatValue();
   }
}