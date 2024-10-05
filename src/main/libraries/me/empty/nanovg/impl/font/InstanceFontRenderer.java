package me.empty.nanovg.impl.font;

import java.awt.*;

public interface InstanceFontRenderer {
    void drawString(String text, float x, float y, Color color);

    void drawStringWithShadow(String text, float x, float y, Color color);

    void drawStringWithGlow(String text, float x, float y, Color color);

    void drawCenteredString(String text, float x, float y, Color color);

    void drawCenteredStringWithShadow(String text, float x, float y, Color color);

    void drawCenteredStringWithGlow(String text, float x, float y, Color color);

    void drawStringCustomShadow(String text, float x, float y, Color color, boolean shadow);

    void drawCenteredStringCustomShadow(String text, float x, float y, Color color, boolean shadow);

    float getStringWidth(String text);

    float getHeight();

    default String trimStringToWidth(String text, float width) {
        return this.trimStringToWidth(text, width, false);
    }

    default String trimStringToWidth(CharSequence text, float width, boolean reverse) {
        StringBuilder trimmedText = new StringBuilder();

        float currentWidth = 0.0F;
        boolean prevIsSpace = false;
        boolean prevIsSpaceOrLineBreak = false;

        for (int index = reverse ? text.length() - 1 : 0; index >= 0 && index < text.length() && currentWidth < width; index += reverse ? -1 : 1) {
            char currentChar = text.charAt(index);
            float charWidth = getStringWidth(String.valueOf(currentChar));

            if (prevIsSpace) {
                prevIsSpace = false;

                if (currentChar != 'l' && currentChar != 'L') {
                    if (currentChar == 'r' || currentChar == 'R') {
                        prevIsSpaceOrLineBreak = false;
                    }
                } else {
                    prevIsSpaceOrLineBreak = true;
                }
            } else if (charWidth < 0.0F) {
                prevIsSpace = true;
            } else {
                currentWidth += charWidth;
                if (prevIsSpaceOrLineBreak) {
                    ++currentWidth;
                }
            }

            if (currentWidth > width) {
                break;
            }

            if (reverse) {
                trimmedText.insert(0, currentChar);
            } else {
                trimmedText.append(currentChar);
            }
        }

        return trimmedText.toString();
    }
}
