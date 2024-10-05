package me.empty.nanovg.impl.draw;

import me.empty.nanovg.impl.util.NanoColorUtil;
import me.empty.nanovg.interfaces.INano;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class NanoRender2DUtil implements INano {
    public static void drawRect(float x, float y, float width, float height, Color color) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRect(nvg, x, y, width, height);
        NanoColorUtil.color(nvg, color.getRGB());
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NanoColorUtil.color(nvg, color.getRGB());
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void drawRound(float x, float y, float width, float height, float radiusLeftUp, float radiusLeftDown, float radiusRightUp, float radiusRightDown, Color color) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, radiusLeftUp, radiusRightUp, radiusRightDown, radiusLeftDown);
        NanoColorUtil.color(nvg, color.getRGB());
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void drawGradientRoundLeftRight(float x, float y, float width, float height, float roundRadius, Color leftColor, Color rightColor) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, roundRadius);
        float gradientY0 = y + height / 2;
        float gradientX1 = x + width;
        NVGPaint imagePaint = NVGPaint.create();
        NVGPaint paint = NanoVG.nvgLinearGradient(nvg, x, gradientY0, gradientX1, gradientY0, NanoColorUtil.color(nvg, leftColor.getRGB()), NanoColorUtil.color(nvg, rightColor.getRGB()), imagePaint);
        NanoVG.nvgFillPaint(nvg, paint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void drawStrokeRound(float x, float y, float width, float height, float roundRadius, float strokeWidth, Color color) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgStrokeColor(nvg, NanoColorUtil.color(nvg, color.getRGB()));
        NanoVG.nvgStrokeWidth(nvg, strokeWidth);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, roundRadius);
        NanoVG.nvgStroke(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void drawDropShadow(float x, float y, float w, float h, float blur, float spread, float cornerRadius, Color color) {
        try (NVGPaint shadowPaint = NVGPaint.calloc();
             NVGColor firstColor = NVGColor.calloc();
             NVGColor secondColor = NVGColor.calloc()
        ) {
            fillNVGColorWithRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f, firstColor);
            fillNVGColorWithRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0, secondColor);

            nvgBoxGradient(nvg, x - spread, y - spread, w + 2 * spread, h + 2 * spread, cornerRadius + spread, blur, firstColor, secondColor, shadowPaint);
            nvgBeginPath(nvg);
            nvgRoundedRect(nvg, x - spread - blur, y - spread - blur, w + 2 * spread + 2 * blur, h + 2 * spread + 2 * blur, cornerRadius + spread);
            nvgPathWinding(nvg, NVG_HOLE);
            nvgFillPaint(nvg, shadowPaint);
            nvgFill(nvg);
            NanoVG.nvgClosePath(nvg);
        }
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgCircle(nvg, x, y, radius);
        NanoColorUtil.color(nvg, color.getRGB());
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public static void fillNVGColorWithRGBA(float r, float g, float b, float a, NVGColor color) {
        color.r(r);
        color.g(g);
        color.b(b);
        color.a(a);
    }
}
