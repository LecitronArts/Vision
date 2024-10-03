/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@ApiStatus.Internal
public final class OptiFineIntegration {

    private static Field of_fast_render;
    private static Field shaderPackLoaded;

    static {
        try {
            of_fast_render = Options.class.getDeclaredField("ofFastRender");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.Shaders");
            shaderPackLoaded = clazz.getDeclaredField("shaderPackLoaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OptiFineIntegration() {
    }

    public static void openShadersGui() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Class<?> clazz = Class.forName("net.optifine.shaders.gui.GuiShaders");
            Constructor<?> constructor = clazz.getConstructor(Screen.class, Options.class);
            minecraft.setScreen((Screen) constructor.newInstance(minecraft.screen, minecraft.options));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Incompatible with TextMC. Because we break the Vanilla rendering order.
     * See TextRenderNode#drawText()  endBatch(Sheets.signSheet()).
     * Modern UI glyph texture is translucent, so ending sign rendering earlier
     * stops sign texture being discarded by depth test.
     */
    public static void setFastRender(boolean fastRender) {
        Minecraft minecraft = Minecraft.getInstance();
        if (of_fast_render != null) {
            try {
                of_fast_render.setBoolean(minecraft.options, fastRender);
                minecraft.options.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setGuiScale(OptionInstance<Integer> option) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Field field = Options.class.getDeclaredField("GUI_SCALE");
            field.setAccessible(true);
            field.set(minecraft.options, option);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isShaderPackLoaded() {
        if (shaderPackLoaded != null) {
            try {
                return shaderPackLoaded.getBoolean(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
