package me.empty.nanovg.impl.draw.image;

import me.empty.nanovg.interfaces.INano;
import org.apache.commons.io.IOUtils;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.nanovg.NanoVG.*;

public class NanoImage implements INano {
    private int imageId = -1;

    public NanoImage(String fileName) {
        InputStream inputStream = NanoImage.class.getResourceAsStream(fileName);
        if (inputStream == null) {
            return;
        }
        ByteBuffer image;
        try {
            byte[] bytes;

            InputStream stream;
            stream = NanoImage.class.getResourceAsStream(fileName);
            if (stream == null) {
                throw new FileNotFoundException(fileName);
            }
            bytes = IOUtils.toByteArray(stream);

            ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder())
                    .put(bytes);
            ((Buffer) data).flip();

            image = data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.imageId = NanoVG.nvgCreateImageMem(nvg, NVG_IMAGE_NEAREST, image);
        if (this.imageId == -1) {
            throw new RuntimeException("Failed to create NanoVG image.");
        }
    }

    public void drawImage(float x, float y, float width, float height) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, x, y, width, height, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRect(nvg, x, y, width, height);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public void drawImage(float x, float y, float width, float height, float imageX, float imageY, float imageWidth, float imageHeight) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, imageX, imageY, imageWidth, imageHeight, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRect(nvg, x, y, width, height);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public void drawImageRounded(float x, float y, float width, float height, float radius) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, x, y, width, height, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public void drawImageRounded(float x, float y, float width, float height, float imageX, float imageY, float imageWidth, float imageHeight, float radius) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, imageX, imageY, imageWidth, imageHeight, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRoundedRect(nvg, x, y, width, height, radius);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public void drawImageVarying(float x, float y, float width, float height, float radiusLeftUp, float radiusLeftDown, float radiusRightUp, float radiusRightDown) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, x, y, width, height, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, radiusLeftUp, radiusRightUp, radiusRightDown, radiusLeftDown);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }

    public void drawImageVarying(float x, float y, float width, float height, float imageX, float imageY, float imageWidth, float imageHeight, float radiusLeftUp, float radiusLeftDown, float radiusRightUp, float radiusRightDown) {
        NVGPaint nvgPaint = NVGPaint.create();
        NanoVG.nvgBeginPath(nvg);
        NanoVG.nvgImagePattern(nvg, imageX, imageY, imageWidth, imageHeight, 0.0f, this.imageId, 1.0f, nvgPaint);
        NanoVG.nvgRoundedRectVarying(nvg, x, y, width, height, radiusLeftUp, radiusRightUp, radiusRightDown, radiusLeftDown);
        NanoVG.nvgFillPaint(nvg, nvgPaint);
        NanoVG.nvgFill(nvg);
        NanoVG.nvgClosePath(nvg);
    }
}
