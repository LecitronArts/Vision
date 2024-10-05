package me.empty.nanovg.impl.font;

import me.empty.nanovg.interfaces.INano;
import me.empty.nanovg.impl.util.NanoColorUtil;
import me.empty.nanovg.impl.draw.NanoRender2DUtil;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static me.empty.nanovg.impl.font.NanoFontHelper.generateColorCodes;
import static me.empty.nanovg.impl.font.NanoFontHelper.loadFontData;
import static org.lwjgl.nanovg.NanoVG.*;

public class NanoFontRenderer implements InstanceFontRenderer, INano {
    // Name of the font
    private final String name;
    // Size of the font
    private final float size;
    // Color code array for the font
    private final int[] colorCode;
    // ByteBuffer to store font data
    protected ByteBuffer buffer;

    /**
     * Constructor to initialize the NanoFontRenderer
     * @param name Name of the font
     * @param file File path of the font data
     * @param size Size of the font
     */
    public NanoFontRenderer(String name, String file, float size) {
        this.name = name;
        this.size = size / 2; // Halving the input size for internal use
        this.colorCode = generateColorCodes(); // Generate color codes
        this.buffer = loadFontData(file); // Load font data from file
        NanoVG.nvgCreateFontMem(nvg, name, this.buffer, false); // Create font in NanoVG
    }

    /**
     * Draws a string without any special effects
     * @param text String to draw
     * @param x X coordinate of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawString(String text, float x, float y, Color color) {
        this.drawString(text, x, y, color, false, false); // Draw string without shadow or glow
    }

    /**
     * Draws a string with a shadow effect
     * @param text String to draw
     * @param x X coordinate of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawStringWithShadow(String text, float x, float y, Color color) {
        this.drawString(text, x, y, color, true, false); // Draw string with shadow
    }

    /**
     * Draws a string with a glow effect
     * @param text String to draw
     * @param x X coordinate of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawStringWithGlow(String text, float x, float y, Color color) {
        this.drawString(text, x, y, color, false, true); // Draw string with glow
    }

    /**
     * Draws a centered string without any special effects
     * @param text String to draw
     * @param x X coordinate of the center of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawCenteredString(String text, float x, float y, Color color) {
        this.drawString(text, x - getStringWidth(text) / 2.0F, y, color); // Center and draw string
    }

    /**
     * Draws a centered string with a shadow effect
     * @param text String to draw
     * @param x X coordinate of the center of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawCenteredStringWithShadow(String text, float x, float y, Color color) {
        this.drawStringWithShadow(text, x - (getStringWidth(text) / 2.0F), y, color); // Center and draw string with shadow
    }

    /**
     * Draws a centered string with a glow effect
     * @param text String to draw
     * @param x X coordinate of the center of the text
     * @param y Y coordinate of the text
     * @param color Color of the text
     */
    @Override
    public void drawCenteredStringWithGlow(String text, float x, float y, Color color) {
        this.drawStringWithGlow(text, x - getStringWidth(text) / 2.0F, y, color); // Center and draw string with glow
    }

    /**
     * Draws a string with custom shadow effect.
     *
     * @param text   the string to draw
     * @param x      the x-coordinate of the string's position
     * @param y      the y-coordinate of the string's position
     * @param color  the color of the string
     * @param shadow whether to draw the string with shadow effect
     */
    @Override
    public void drawStringCustomShadow(String text, float x, float y, Color color, boolean shadow) {
        if (shadow) {
            this.drawStringWithShadow(text, x, y, color);
        } else {
            this.drawString(text, x, y, color);
        }
    }

    /**
     * Draws a centered string with custom shadow effect.
     *
     * @param text   the string to draw centered
     * @param x      the x-coordinate of the center position
     * @param y      the y-coordinate of the center position
     * @param color  the color of the string
     * @param shadow whether to draw the string with shadow effect
     */
    @Override
    public void drawCenteredStringCustomShadow(String text, float x, float y, Color color, boolean shadow) {
        if (shadow) {
            this.drawCenteredStringWithShadow(text, x, y, color);
        } else {
            this.drawCenteredString(text, x, y, color);
        }
    }

    /**
     * Calculates the width of the given string.
     * Special characters starting with '§' are ignored in the width calculation.
     *
     * @param text the string to measure
     * @return the width of the string in pixels
     */
    @Override
    public float getStringWidth(String text) {
        if (text.contains("§")) {
            for (int i = 0; i < text.length(); i++) {
                String s = String.valueOf(text.charAt(i));
                if (s.equals("§") && i + 1 < text.length()) {
                    text = text.replace(s + text.charAt(i + 1), "");
                }
            }
        }
        float width;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            NanoVG.nvgFontFace(nvg, name);
            NanoVG.nvgFontSize(nvg, size);
            FloatBuffer floatBuffer = memoryStack.mallocFloat(4);
            width = NanoVG.nvgTextBounds(nvg, 0.0f, 0.0f, text, floatBuffer);
        }
        return width;
    }

    /**
     * Gets the height of the font.
     *
     * @return the height of the font in pixels
     */
    @Override
    public float getHeight() {
        NanoVG.nvgFontSize(nvg, size);
        float[] ascender = new float[1];
        float[] descender = new float[1];
        float[] lineh = new float[1];
        NanoVG.nvgFontFace(nvg, name);
        NanoVG.nvgTextMetrics(nvg, ascender, descender, lineh);
        return lineh[0];
    }

    /**
     * Draws a string on the canvas with optional shadow and glow effects.
     *
     * @param text   the string to draw
     * @param x      the x-coordinate of the string's position
     * @param y      the y-coordinate of the string's position
     * @param color  the color of the string
     * @param shadow whether to draw the string with a shadow effect
     * @param glow   whether to draw the string with a glow effect
     * <p>
     * This method renders the specified string at the given position on the canvas.
     * It allows for optional visual effects such as shadow and glow to be applied
     * to the string, enhancing its visibility and appearance.
     */
    private void drawString(String text, float x, float y, Color color, boolean shadow, boolean glow) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgFontSize(nvg, size);
        NanoVG.nvgFontFace(nvg, name);
        NanoVG.nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        boolean bold = false;
        boolean strikethrough = false;
        boolean underline = false;

        int currentColor = color.getRGB();

        if (text.contains("§")) {
            float offset = 0F;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '§' && i + 1 < text.length()) {
                    char code = text.charAt(i + 1);
                    int colorIndex = "0123456789abcdefklmnor".indexOf(code);
                    switch (colorIndex) {
                        case 1 :
                        case 2 :
                        case 3 :
                        case 4 :
                        case 5 :
                        case 6 :
                        case 7 :
                        case 8 :
                        case 9 :
                        case 10 :
                        case 11 :
                        case 12 :
                        case 13 :
                        case 14 :
                        case 15 : {
                            int colorRGB = colorCode[colorIndex];
                            currentColor = ((0xFF) << 24) | ((colorRGB >> 16 & 0xFF) << 16) | ((colorRGB >> 8 & 0xFF) << 8) | (colorRGB & 0xFF);
                            break;
                        }
                        case 17 : {
                            bold = true;
                            break;
                        }
                        case 18: {
                            strikethrough = true;
                            break;
                        }
                        case 19: {
                            underline = true;
                            break;
                        }
                        case 16:
                        case 20:
                        case 21:
                        default: {
                            bold = false;
                            underline = false;
                            strikethrough = false;
                            currentColor = color.getRGB();
                        }
                    }
                    i++;
                    continue;
                }

                String currentText = String.valueOf(c);
                if (glow) {
                    this.drawGlow(currentText, x + offset, y, size, new Color(currentColor));
                }
                this.drawNanoFont(shadow, new Color(currentColor), currentText, x + offset, y, size, bold);
                if (strikethrough) {
                    NanoRender2DUtil.drawRect(x + offset, y + (this.getHeight() - 2) / 2, this.getStringWidth(currentText), 1F, new Color(-1));
                }
                if (underline) {
                    NanoRender2DUtil.drawRect(x + offset, y + this.getHeight() - 2, this.getStringWidth(currentText), 0.3F, new Color(-1));
                }
                offset += this.getStringWidth(currentText);
            }
        } else {
            if (glow) {
                this.drawGlow(text, x, y, size, new Color(currentColor));
            }
            this.drawNanoFont(shadow, new Color(currentColor), text, x, y, size, false);
        }
        NanoVG.nvgClosePath(nvg);
    }

    /**
     * Draws text using a nano-sized font with optional shadow and bold effects.
     *
     * @param shadow  whether to draw the text with a shadow effect
     * @param color   the color of the text
     * @param text    the string to draw
     * @param x       the x-coordinate of the text's position
     * @param y       the y-coordinate of the text's baseline position (adjusted for size)
     * @param size    the size of the font
     * @param bold    whether to draw the text in bold
     * <p>
     * This method uses a nano-graphics library (assumed to be NanoVG based on method names)
     * to render text with a specified size, color, and optional shadow and bold effects.
     * If shadow is enabled, a darker version of the text color is used to draw a shadow
     * slightly offset from the text. Bold text is achieved by drawing the text twice with
     * slight offsets.
     */
    private void drawNanoFont(boolean shadow, Color color, String text, float x, float y, float size, boolean bold) {
        if (shadow) {
            Color shadowColor = new Color((color.getRGB() & 16579836) >> 2 | color.getRGB() & -16777216);
            NanoColorUtil.color(nvg, shadowColor.getRGB());
            if (bold) {
                NanoVG.nvgText(nvg, x + 1F, (y + 0.5F) + size / 2, text);
            }
            NanoVG.nvgText(nvg, x + 0.5F, (y + 0.5F) + size / 2, text);
        }
        NanoColorUtil.color(nvg, color.getRGB());
        if (bold) {
            NanoVG.nvgText(nvg, x + 0.5F, y + size / 2, text);
        }
        NanoVG.nvgText(nvg, x, y + size / 2, text);
    }

    /**
     * Draws text with a glow effect around it.
     *
     * @param text    the string to draw
     * @param x       the x-coordinate of the text's position
     * @param y       the y-coordinate of the text's baseline position (adjusted for size)
     * @param size    the size of the font
     * @param color   the color of the text and its glow
     * <p>
     * This method creates a glow effect around the text by drawing multiple layers of the
     * text with slightly offset positions and decreasing alpha values. The number of layers
     * and the glow radius can be adjusted to achieve different visual effects.
     * The final layer is drawn with the original color and position.
     */
    private void drawGlow(String text, float x, float y, float size, Color color) {
        int numLayers = 10;
        float glowRadius = 3.0f;
        for (int i = 0; i < numLayers; i++) {
            float alpha = 0.1f * (1 - ((float) i / numLayers));
            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha));
            NanoColorUtil.color(nvg, glowColor.getRGB());
            float offset = glowRadius * ((float) i / numLayers);
            NanoVG.nvgText(nvg, x - offset, y - offset + size / 2, text);
            NanoVG.nvgText(nvg, x + offset, y - offset + size / 2, text);
            NanoVG.nvgText(nvg, x - offset, y + offset + size / 2, text);
            NanoVG.nvgText(nvg, x + offset, y + offset + size / 2, text);
        }
        NanoColorUtil.color(nvg, color.getRGB());
        NanoVG.nvgText(nvg, x, y + size / 2, text);
    }
}
