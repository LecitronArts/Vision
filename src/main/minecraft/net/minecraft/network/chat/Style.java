package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class Style {
   public static final Style EMPTY = new Style((TextColor)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (ClickEvent)null, (HoverEvent)null, (String)null, (ResourceLocation)null);
   public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
   @Nullable
   final TextColor color;
   @Nullable
   final Boolean bold;
   @Nullable
   final Boolean italic;
   @Nullable
   final Boolean underlined;
   @Nullable
   final Boolean strikethrough;
   @Nullable
   final Boolean obfuscated;
   @Nullable
   final ClickEvent clickEvent;
   @Nullable
   final HoverEvent hoverEvent;
   @Nullable
   final String insertion;
   @Nullable
   final ResourceLocation font;

   private static Style create(Optional<TextColor> pColor, Optional<Boolean> pBold, Optional<Boolean> pItalic, Optional<Boolean> pUnderlined, Optional<Boolean> pStrikethrough, Optional<Boolean> pObfuscated, Optional<ClickEvent> pClickEvent, Optional<HoverEvent> pHoverEvent, Optional<String> pInsertion, Optional<ResourceLocation> pFont) {
      Style style = new Style(pColor.orElse((TextColor)null), pBold.orElse((Boolean)null), pItalic.orElse((Boolean)null), pUnderlined.orElse((Boolean)null), pStrikethrough.orElse((Boolean)null), pObfuscated.orElse((Boolean)null), pClickEvent.orElse((ClickEvent)null), pHoverEvent.orElse((HoverEvent)null), pInsertion.orElse((String)null), pFont.orElse((ResourceLocation)null));
      return style.equals(EMPTY) ? EMPTY : style;
   }

   private Style(@Nullable TextColor pColor, @Nullable Boolean pBold, @Nullable Boolean pItalic, @Nullable Boolean pUnderlined, @Nullable Boolean pStrikethrough, @Nullable Boolean pObfuscated, @Nullable ClickEvent pClickEvent, @Nullable HoverEvent pHoverEvent, @Nullable String pInsertion, @Nullable ResourceLocation pFont) {
      this.color = pColor;
      this.bold = pBold;
      this.italic = pItalic;
      this.underlined = pUnderlined;
      this.strikethrough = pStrikethrough;
      this.obfuscated = pObfuscated;
      this.clickEvent = pClickEvent;
      this.hoverEvent = pHoverEvent;
      this.insertion = pInsertion;
      this.font = pFont;
   }

   @Nullable
   public TextColor getColor() {
      return this.color;
   }

   public boolean isBold() {
      return this.bold == Boolean.TRUE;
   }

   public boolean isItalic() {
      return this.italic == Boolean.TRUE;
   }

   public boolean isStrikethrough() {
      return this.strikethrough == Boolean.TRUE;
   }

   public boolean isUnderlined() {
      return this.underlined == Boolean.TRUE;
   }

   public boolean isObfuscated() {
      return this.obfuscated == Boolean.TRUE;
   }

   public boolean isEmpty() {
      return this == EMPTY;
   }

   @Nullable
   public ClickEvent getClickEvent() {
      return this.clickEvent;
   }

   @Nullable
   public HoverEvent getHoverEvent() {
      return this.hoverEvent;
   }

   @Nullable
   public String getInsertion() {
      return this.insertion;
   }

   public ResourceLocation getFont() {
      return this.font != null ? this.font : DEFAULT_FONT;
   }

   private static <T> Style checkEmptyAfterChange(Style pStyle, @Nullable T pOldValue, @Nullable T pNewValue) {
      return pOldValue != null && pNewValue == null && pStyle.equals(EMPTY) ? EMPTY : pStyle;
   }

   public Style withColor(@Nullable TextColor pColor) {
      return Objects.equals(this.color, pColor) ? this : checkEmptyAfterChange(new Style(pColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.color, pColor);
   }

   public Style withColor(@Nullable ChatFormatting pFormatting) {
      return this.withColor(pFormatting != null ? TextColor.fromLegacyFormat(pFormatting) : null);
   }

   public Style withColor(int pRgb) {
      return this.withColor(TextColor.fromRgb(pRgb));
   }

   public Style withBold(@Nullable Boolean pBold) {
      return Objects.equals(this.bold, pBold) ? this : checkEmptyAfterChange(new Style(this.color, pBold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.bold, pBold);
   }

   public Style withItalic(@Nullable Boolean pItalic) {
      return Objects.equals(this.italic, pItalic) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, pItalic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.italic, pItalic);
   }

   public Style withUnderlined(@Nullable Boolean pUnderlined) {
      return Objects.equals(this.underlined, pUnderlined) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, pUnderlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.underlined, pUnderlined);
   }

   public Style withStrikethrough(@Nullable Boolean pStrikethrough) {
      return Objects.equals(this.strikethrough, pStrikethrough) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, pStrikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.strikethrough, pStrikethrough);
   }

   public Style withObfuscated(@Nullable Boolean pObfuscated) {
      return Objects.equals(this.obfuscated, pObfuscated) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, pObfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font), this.obfuscated, pObfuscated);
   }

   public Style withClickEvent(@Nullable ClickEvent pClickEvent) {
      return Objects.equals(this.clickEvent, pClickEvent) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, pClickEvent, this.hoverEvent, this.insertion, this.font), this.clickEvent, pClickEvent);
   }

   public Style withHoverEvent(@Nullable HoverEvent pHoverEvent) {
      return Objects.equals(this.hoverEvent, pHoverEvent) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, pHoverEvent, this.insertion, this.font), this.hoverEvent, pHoverEvent);
   }

   public Style withInsertion(@Nullable String pInsertion) {
      return Objects.equals(this.insertion, pInsertion) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, pInsertion, this.font), this.insertion, pInsertion);
   }

   public Style withFont(@Nullable ResourceLocation pFontId) {
      return Objects.equals(this.font, pFontId) ? this : checkEmptyAfterChange(new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, pFontId), this.font, pFontId);
   }

   public Style applyFormat(ChatFormatting pFormatting) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch (pFormatting) {
         case OBFUSCATED:
            obool4 = true;
            break;
         case BOLD:
            obool = true;
            break;
         case STRIKETHROUGH:
            obool2 = true;
            break;
         case UNDERLINE:
            obool3 = true;
            break;
         case ITALIC:
            obool1 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            textcolor = TextColor.fromLegacyFormat(pFormatting);
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyLegacyFormat(ChatFormatting pFormatting) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch (pFormatting) {
         case OBFUSCATED:
            obool4 = true;
            break;
         case BOLD:
            obool = true;
            break;
         case STRIKETHROUGH:
            obool2 = true;
            break;
         case UNDERLINE:
            obool3 = true;
            break;
         case ITALIC:
            obool1 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            obool4 = false;
            obool = false;
            obool2 = false;
            obool3 = false;
            obool1 = false;
            textcolor = TextColor.fromLegacyFormat(pFormatting);
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyFormats(ChatFormatting... pFormats) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;

      for(ChatFormatting chatformatting : pFormats) {
         switch (chatformatting) {
            case OBFUSCATED:
               obool4 = true;
               break;
            case BOLD:
               obool = true;
               break;
            case STRIKETHROUGH:
               obool2 = true;
               break;
            case UNDERLINE:
               obool3 = true;
               break;
            case ITALIC:
               obool1 = true;
               break;
            case RESET:
               return EMPTY;
            default:
               textcolor = TextColor.fromLegacyFormat(chatformatting);
         }
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyTo(Style pStyle) {
      if (this == EMPTY) {
         return pStyle;
      } else {
         return pStyle == EMPTY ? this : new Style(this.color != null ? this.color : pStyle.color, this.bold != null ? this.bold : pStyle.bold, this.italic != null ? this.italic : pStyle.italic, this.underlined != null ? this.underlined : pStyle.underlined, this.strikethrough != null ? this.strikethrough : pStyle.strikethrough, this.obfuscated != null ? this.obfuscated : pStyle.obfuscated, this.clickEvent != null ? this.clickEvent : pStyle.clickEvent, this.hoverEvent != null ? this.hoverEvent : pStyle.hoverEvent, this.insertion != null ? this.insertion : pStyle.insertion, this.font != null ? this.font : pStyle.font);
      }
   }

   public String toString() {
      final StringBuilder stringbuilder = new StringBuilder("{");

      class Collector {
         private boolean isNotFirst;

         private void prependSeparator() {
            if (this.isNotFirst) {
               stringbuilder.append(',');
            }

            this.isNotFirst = true;
         }

         void addFlagString(String p_237290_, @Nullable Boolean p_237291_) {
            if (p_237291_ != null) {
               this.prependSeparator();
               if (!p_237291_) {
                  stringbuilder.append('!');
               }

               stringbuilder.append(p_237290_);
            }

         }

         void addValueString(String p_237293_, @Nullable Object p_237294_) {
            if (p_237294_ != null) {
               this.prependSeparator();
               stringbuilder.append(p_237293_);
               stringbuilder.append('=');
               stringbuilder.append(p_237294_);
            }

         }
      }

      Collector style$1collector = new Collector();
      style$1collector.addValueString("color", this.color);
      style$1collector.addFlagString("bold", this.bold);
      style$1collector.addFlagString("italic", this.italic);
      style$1collector.addFlagString("underlined", this.underlined);
      style$1collector.addFlagString("strikethrough", this.strikethrough);
      style$1collector.addFlagString("obfuscated", this.obfuscated);
      style$1collector.addValueString("clickEvent", this.clickEvent);
      style$1collector.addValueString("hoverEvent", this.hoverEvent);
      style$1collector.addValueString("insertion", this.insertion);
      style$1collector.addValueString("font", this.font);
      stringbuilder.append("}");
      return stringbuilder.toString();
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof Style)) {
         return false;
      } else {
         Style style = (Style)pOther;
         return this.bold == style.bold && Objects.equals(this.getColor(), style.getColor()) && this.italic == style.italic && this.obfuscated == style.obfuscated && this.strikethrough == style.strikethrough && this.underlined == style.underlined && Objects.equals(this.clickEvent, style.clickEvent) && Objects.equals(this.hoverEvent, style.hoverEvent) && Objects.equals(this.insertion, style.insertion) && Objects.equals(this.font, style.font);
      }
   }

   public int hashCode() {
      return Objects.hash(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion);
   }

   public static class Serializer {
      public static final MapCodec<Style> MAP_CODEC = RecordCodecBuilder.mapCodec((p_312892_) -> {
         return p_312892_.group(ExtraCodecs.strictOptionalField(TextColor.CODEC, "color").forGetter((p_311313_) -> {
            return Optional.ofNullable(p_311313_.color);
         }), ExtraCodecs.strictOptionalField(Codec.BOOL, "bold").forGetter((p_310279_) -> {
            return Optional.ofNullable(p_310279_.bold);
         }), ExtraCodecs.strictOptionalField(Codec.BOOL, "italic").forGetter((p_310016_) -> {
            return Optional.ofNullable(p_310016_.italic);
         }), ExtraCodecs.strictOptionalField(Codec.BOOL, "underlined").forGetter((p_312012_) -> {
            return Optional.ofNullable(p_312012_.underlined);
         }), ExtraCodecs.strictOptionalField(Codec.BOOL, "strikethrough").forGetter((p_310101_) -> {
            return Optional.ofNullable(p_310101_.strikethrough);
         }), ExtraCodecs.strictOptionalField(Codec.BOOL, "obfuscated").forGetter((p_310873_) -> {
            return Optional.ofNullable(p_310873_.obfuscated);
         }), ExtraCodecs.strictOptionalField(ClickEvent.CODEC, "clickEvent").forGetter((p_312594_) -> {
            return Optional.ofNullable(p_312594_.clickEvent);
         }), ExtraCodecs.strictOptionalField(HoverEvent.CODEC, "hoverEvent").forGetter((p_311111_) -> {
            return Optional.ofNullable(p_311111_.hoverEvent);
         }), ExtraCodecs.strictOptionalField(Codec.STRING, "insertion").forGetter((p_310639_) -> {
            return Optional.ofNullable(p_310639_.insertion);
         }), ExtraCodecs.strictOptionalField(ResourceLocation.CODEC, "font").forGetter((p_310574_) -> {
            return Optional.ofNullable(p_310574_.font);
         })).apply(p_312892_, Style::create);
      });
      public static final Codec<Style> CODEC = MAP_CODEC.codec();
   }
}