package net.minecraft.network.chat.contents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public interface PlainTextContents extends ComponentContents {
   MapCodec<PlainTextContents> CODEC = RecordCodecBuilder.mapCodec((p_310759_) -> {
      return p_310759_.group(Codec.STRING.fieldOf("text").forGetter(PlainTextContents::text)).apply(p_310759_, PlainTextContents::create);
   });
   ComponentContents.Type<PlainTextContents> TYPE = new ComponentContents.Type<>(CODEC, "text");
   PlainTextContents EMPTY = new PlainTextContents() {
      public String toString() {
         return "empty";
      }

      public String text() {
         return "";
      }
   };

   static PlainTextContents create(String p_310243_) {
      return (PlainTextContents)(p_310243_.isEmpty() ? EMPTY : new PlainTextContents.LiteralContents(p_310243_));
   }

   String text();

   default ComponentContents.Type<?> type() {
      return TYPE;
   }

   public static record LiteralContents(String text) implements PlainTextContents {
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_312000_) {
         return p_312000_.accept(this.text);
      }

      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_313135_, Style p_310796_) {
         return p_313135_.accept(p_310796_, this.text);
      }

      public String toString() {
         return "literal{" + this.text + "}";
      }

      public String text() {
         return this.text;
      }
   }
}