package me.empty.nanovg.impl.font;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class NanoFontHelper {
    public static int[] generateColorCodes() {
        int[] colorCode = new int[32];
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index & 0x1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }

            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
        return colorCode;
    }

    public static ByteBuffer loadFontData(String fileName) {
        try (InputStream stream = NanoFontHelper.class.getResourceAsStream(fileName)) {
            if (stream == null) {
                throw new FileNotFoundException(fileName);
            }
            byte[] bytes = IOUtils.toByteArray(stream);
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder()).put(bytes);
            ((Buffer) buffer).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font data: " + fileName, e);
        }
    }
}