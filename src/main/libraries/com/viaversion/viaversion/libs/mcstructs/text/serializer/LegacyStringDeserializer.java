package com.viaversion.viaversion.libs.mcstructs.text.serializer;

import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;

import java.util.function.Function;

/**
 * Deserialize a legacy formatted string to an {@link ATextComponent}.
 */
public class LegacyStringDeserializer {

    /**
     * Parse a string with legacy formatting codes into an {@link ATextComponent}.<br>
     * Minecraft 1.13+ ignores unknown formatting codes. Earlier versions will handle them like {@link TextFormatting#WHITE}.
     *
     * @param s            The string to parse
     * @param unknownWhite Handle unknown formatting codes as reset
     * @return The parsed string
     */
    public static ATextComponent parse(final String s, final boolean unknownWhite) {
        return parse(s, TextFormatting.COLOR_CHAR, unknownWhite);
    }

    /**
     * Parse a string with legacy formatting codes into an {@link ATextComponent}.<br>
     * Minecraft 1.13+ ignores unknown formatting codes. Earlier versions will handle them like {@link TextFormatting#WHITE}.
     *
     * @param s            The string to parse
     * @param colorChar    The color char to use (e.g. {@link TextFormatting#COLOR_CHAR})
     * @param unknownWhite Handle unknown formatting codes as reset
     * @return The parsed string
     */
    public static ATextComponent parse(final String s, final char colorChar, final boolean unknownWhite) {
        return parse(s, colorChar, c -> {
            TextFormatting formatting = TextFormatting.getByCode(c);
            if (formatting == null) {
                if (unknownWhite) return TextFormatting.WHITE;
                else return null;
            }
            return formatting;
        });
    }

    /**
     * Parse a string with legacy formatting codes into an {@link ATextComponent}.<br>
     * The {@code formattingResolver} should return a formatting for the given char or {@code null} if the previous formatting should be kept.<br>
     * When returning a color the previous formattings like {@code bold, italic, etc.} will be reset.
     *
     * @param s                  The string to parse
     * @param colorChar          The color char to use (e.g. {@link TextFormatting#COLOR_CHAR})
     * @param formattingResolver The function that resolves the formatting for the given char
     * @return The parsed string
     */
    public static ATextComponent parse(final String s, final char colorChar, final Function<Character, TextFormatting> formattingResolver) {
        char[] chars = s.toCharArray();
        Style style = new Style();
        StringBuilder currentPart = new StringBuilder();
        ATextComponent out = new StringComponent("");

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == colorChar) {
                if (i + 1 < chars.length) {
                    char format = chars[++i];
                    TextFormatting formatting = formattingResolver.apply(format);
                    if (formatting == null) continue;

                    if (currentPart.length() != 0) {
                        out.append(new StringComponent(currentPart.toString()).setStyle(style.copy()));
                        currentPart = new StringBuilder();
                        if (formatting.isColor() || TextFormatting.RESET.equals(formatting)) style = new Style();
                    }
                    style.setFormatting(formatting);
                }
            } else {
                currentPart.append(c);
            }
        }
        if (currentPart.length() != 0) out.append(new StringComponent(currentPart.toString()).setStyle(style));
        if (out.getSiblings().size() == 1) return out.getSiblings().get(0);
        return out;
    }

}
