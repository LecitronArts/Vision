/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

import static icyllis.modernui.ModernUI.*;

/**
 * Mod class, common only.
 */
public abstract class ModernUIMod {

    // false to disable extensions
    public static final String BOOTSTRAP_DISABLE_TEXT_ENGINE = "modernui_mc_disableTextEngine";
    public static final String BOOTSTRAP_DISABLE_SMOOTH_SCROLLING = "modernui_mc_disableSmoothScrolling";
    public static final String BOOTSTRAP_DISABLE_ENHANCED_TEXT_FIELD = "modernui_mc_disableEnhancedTextField";

    public static volatile boolean sDevelopment;
    public static volatile boolean sDeveloperMode;

    private static volatile Boolean sTextEngineEnabled;


    @Nonnull
    public static ResourceLocation location(String path) {
        return new ResourceLocation(ID, path);
    }

    public static boolean isOptiFineLoaded() {
        return true;
    }

    public static boolean isDeveloperMode() {
        return sDeveloperMode || sDevelopment;
    }

    // check if it is on client side before calling
    public static String getBootstrapProperty(String key) {
        return ModernUIClient.getBootstrapProperty(key);
    }

    /**
     * Returns true is text engine is or will be enabled this game run.
     * Return value won't alter even if bootstrap property is changed at runtime.
     * This method is thread-safe, check if it is on client side before calling.
     *
     * @since 3.10.1
     */
    public static boolean isTextEngineEnabled() {
        // this method should be first called from MixinConfigPlugin
        if (sTextEngineEnabled == null) {
            synchronized (ModernUIMod.class) {
                if (sTextEngineEnabled == null) {
                    sTextEngineEnabled = !Boolean.parseBoolean(
                            getBootstrapProperty(BOOTSTRAP_DISABLE_TEXT_ENGINE)
                    );
                }
            }
        }
        return sTextEngineEnabled;
    }
}
