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

package icyllis.modernui.mc.fabric;

import com.mojang.blaze3d.platform.*;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.Handler;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.text.LineBreakConfig;
import icyllis.modernui.mc.*;
import icyllis.modernui.mc.text.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewConfiguration;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static icyllis.modernui.ModernUI.*;

@ApiStatus.Internal
public final class Config {/*
    Client CLIENT = new Client();

    public static class Client {

        public static final int ANIM_DURATION_MIN = 0;
        public static final int ANIM_DURATION_MAX = 800;
        public static final int BLUR_RADIUS_MIN = 0;
        public static final int BLUR_RADIUS_MAX = 18;
        public static final float FONT_SCALE_MIN = 0.5f;
        public static final float FONT_SCALE_MAX = 2.0f;
        public static final int TOOLTIP_BORDER_COLOR_ANIM_MIN = 0;
        public static final int TOOLTIP_BORDER_COLOR_ANIM_MAX = 5000;
        public static final float TOOLTIP_BORDER_WIDTH_MIN = 0.5f;
        public static final float TOOLTIP_BORDER_WIDTH_MAX = 2.5f;
        public static final float TOOLTIP_CORNER_RADIUS_MIN = 0;
        public static final float TOOLTIP_CORNER_RADIUS_MAX = 8;
        public static final float TOOLTIP_SHADOW_RADIUS_MIN = 0;
        public static final float TOOLTIP_SHADOW_RADIUS_MAX = 32;
        public static final int TOOLTIP_ARROW_SCROLL_FACTOR_MIN = 0;
        public static final int TOOLTIP_ARROW_SCROLL_FACTOR_MAX = 320;
        
        public final boolean mBlurEffect;
        public final boolean mBlurWithBackground;
        public final int mBackgroundDuration;
        public final int mBlurRadius;
        public final List<String> mBackgroundColor;
        public final boolean mInventoryPause;
        public final boolean mTooltip;
        public final boolean mRoundedTooltip;
        public final boolean mCenterTooltipTitle;
        public final boolean mTooltipTitleBreak;
        public final boolean mExactTooltipPositioning;
        public final List<String> mTooltipFill;
        public final List<String> mTooltipStroke;
        public final int mTooltipCycle;
        public final double mTooltipWidth;
        public final double mTooltipRadius;
        public final double mTooltipShadowRadius;
        public final double mTooltipShadowAlpha;
        public final boolean mAdaptiveTooltipColors;
        public final int mTooltipArrowScrollFactor;
        //public final int mTooltipDuration;
        public final boolean mDing;
        //public final boolean mZoom;
        //private final boolean hudBars;
        public final boolean mForceRtl;
        public final double mFontScale;
        public WindowMode mWindowMode;
        public final boolean mUseNewGuiScale;
        //public final boolean mRemoveSignature;
        public final boolean mRemoveTelemetry;
        //public final boolean mSecurePublicKey;
        public final int mFramerateInactive;
        public final int mFramerateMinimized;
        public final double mMasterVolumeInactive;
        public final double mMasterVolumeMinimized;

        public final int mScrollbarSize;
        public final int mTouchSlop;
        public final int mMinScrollbarTouchTarget;
        public final int mMinimumFlingVelocity;
        public final int mMaximumFlingVelocity;
        public final int mOverscrollDistance;
        public final int mOverflingDistance;
        public final double mVerticalScrollFactor;
        public final double mHorizontalScrollFactor;

        private final List<String> mBlurBlacklist;

        public final String mFirstFontFamily;
        public final List<String> mFallbackFontFamilyList;
        public final List<String> mFontRegistrationList;
        public final boolean mUseColorEmoji;
        public final boolean mEmojiShortcodes;

        /*public final boolean mBlurEffect;
        public final boolean mBlurWithBackground;
        public final int mBackgroundDuration;
        public final int mBlurRadius;
        public final List<String> mBackgroundColor;
        public final boolean mInventoryPause;
        public final boolean mTooltip;
        public final boolean mRoundedTooltip;
        public final boolean mCenterTooltipTitle;
        public final boolean mTooltipTitleBreak;
        public final boolean mExactTooltipPositioning;
        public final List<String> mTooltipFill;
        public final List<String> mTooltipStroke;
        public final int mTooltipCycle;
        public final double mTooltipWidth;
        public final double mTooltipRadius;
        public final double mTooltipShadowRadius;
        public final double mTooltipShadowAlpha;
        public final boolean mAdaptiveTooltipColors;
        public final int mTooltipArrowScrollFactor;
        //public final int mTooltipDuration;
        public final boolean mDing;
        //public final boolean mZoom;
        //private final boolean hudBars;
        public final boolean mForceRtl;
        public final double mFontScale;
        public final ForgeConfigSpec.EnumValue<WindowMode> mWindowMode;
        public final boolean mUseNewGuiScale;
        //public final boolean mRemoveSignature;
        public final boolean mRemoveTelemetry;
        //public final boolean mSecurePublicKey;
        public final int mFramerateInactive;
        public final int mFramerateMinimized;
        public final double mMasterVolumeInactive;
        public final double mMasterVolumeMinimized;

        public final int mScrollbarSize;
        public final int mTouchSlop;
        public final int mMinScrollbarTouchTarget;
        public final int mMinimumFlingVelocity;
        public final int mMaximumFlingVelocity;
        public final int mOverscrollDistance;
        public final int mOverflingDistance;
        public final double mVerticalScrollFactor;
        public final double mHorizontalScrollFactor;

        private final List<String> mBlurBlacklist;

        public final ForgeConfigSpec.ConfigValue<String> mFirstFontFamily;
        public final List<String> mFallbackFontFamilyList;
        public final List<String> mFontRegistrationList;
        public final boolean mUseColorEmoji;
        public final boolean mEmojiShortcodes;
        


        public WindowMode mLastWindowMode = WindowMode.NORMAL;
        

        private void reload() {
            BlurHandler.sBlurEffect = mBlurEffect;
            BlurHandler.sBlurWithBackground = mBlurWithBackground;
            BlurHandler.sBackgroundDuration = mBackgroundDuration;
            BlurHandler.sBlurRadius = mBlurRadius;

            BlurHandler.sFramerateInactive = mFramerateInactive;
            BlurHandler.sFramerateMinimized = Math.min(
                    mFramerateMinimized,
                    BlurHandler.sFramerateInactive
            );
            BlurHandler.sMasterVolumeInactive = (float) mMasterVolumeInactive;
            BlurHandler.sMasterVolumeMinimized = (float) Math.min(
                    mMasterVolumeMinimized,
                    BlurHandler.sMasterVolumeInactive
            );

            List<? extends String> inColors = mBackgroundColor;
            int[] resultColors = new int[4];
            int color = 0x99000000;
            for (int i = 0; i < 4; i++) {
                if (inColors != null && i < inColors.size()) {
                    String s = inColors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for screen background, index: {}", i, e);
                    }
                }
                resultColors[i] = color;
            }
            BlurHandler.sBackgroundColor = resultColors;

            BlurHandler.INSTANCE.loadBlacklist(mBlurBlacklist);

            ModernUIClient.sInventoryPause = mInventoryPause;
            //ModernUIForge.sRemoveMessageSignature = mRemoveSignature;
            ModernUIClient.sRemoveTelemetrySession = mRemoveTelemetry;
            //ModernUIForge.sSecureProfilePublicKey = mSecurePublicKey;

            TooltipRenderer.sTooltip = mTooltip;

            inColors = mTooltipFill;
            color = 0xFFFFFFFF;
            for (int i = 0; i < 4; i++) {
                if (inColors != null && i < inColors.size()) {
                    String s = inColors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for tooltip background, index: {}", i, e);
                    }
                }
                TooltipRenderer.sFillColor[i] = color;
            }
            inColors = mTooltipStroke;
            color = 0xFFFFFFFF;
            for (int i = 0; i < 4; i++) {
                if (inColors != null && i < inColors.size()) {
                    String s = inColors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for tooltip border, index: {}", i, e);
                    }
                }
                TooltipRenderer.sStrokeColor[i] = color;
            }
            //TooltipRenderer.sAnimationDuration = mTooltipDuration;
            TooltipRenderer.sBorderColorCycle = mTooltipCycle;
            TooltipRenderer.sExactPositioning = mExactTooltipPositioning;
            TooltipRenderer.sRoundedShapes = mRoundedTooltip;
            TooltipRenderer.sCenterTitle = mCenterTooltipTitle;
            TooltipRenderer.sTitleBreak = mTooltipTitleBreak;
            TooltipRenderer.sBorderWidth = (float) mTooltipWidth;
            TooltipRenderer.sCornerRadius = (float) mTooltipRadius;
            TooltipRenderer.sShadowRadius = (float) mTooltipShadowRadius;
            TooltipRenderer.sShadowAlpha = (float) mTooltipShadowAlpha;
            TooltipRenderer.sAdaptiveColors = mAdaptiveTooltipColors;
            TooltipRenderer.sArrowScrollFactor = mTooltipArrowScrollFactor;

            UIManager.sDingEnabled = mDing;
            //UIManager.sZoomEnabled = mZoom && !ModernUIMod.isOptiFineLoaded();

            WindowMode windowMode = mWindowMode;
            if (mLastWindowMode != windowMode) {
                mLastWindowMode = windowMode;
                Minecraft.getInstance().tell(() -> mLastWindowMode.apply());
            }

            //TestHUD.sBars = hudBars;
            Handler handler = Core.getUiHandlerAsync();
            if (handler != null) {
                handler.post(() -> {
                    UIManager.getInstance().updateLayoutDir(mForceRtl);
                    ModernUIClient.sFontScale = (float) mFontScale;
                    var ctx = ModernUI.getInstance();
                    if (ctx != null) {
                        Resources res = ctx.getResources();
                        DisplayMetrics metrics = new DisplayMetrics();
                        metrics.setTo(res.getDisplayMetrics());
                        metrics.scaledDensity = ModernUIClient.sFontScale * metrics.density;
                        res.updateMetrics(metrics);
                    }
                });
            }

            ModernUIClient.sUseColorEmoji = mUseColorEmoji;
            ModernUIClient.sEmojiShortcodes = mEmojiShortcodes;
            ModernUIClient.sFirstFontFamily = mFirstFontFamily;
            ModernUIClient.sFallbackFontFamilyList = mFallbackFontFamilyList;
            ModernUIClient.sFontRegistrationList = mFontRegistrationList;

            // scan and preload typeface in background thread
            //ModernUI.getSelectedTypeface();
        }

        public enum WindowMode {
            NORMAL,
            FULLSCREEN,
            FULLSCREEN_BORDERLESS,
            MAXIMIZED,
            MAXIMIZED_BORDERLESS,
            WINDOWED,
            WINDOWED_BORDERLESS;

            public void apply() {
                if (this == NORMAL) {
                    return;
                }
                Window window = Minecraft.getInstance().getWindow();
                switch (this) {
                    case FULLSCREEN -> {
                        if (!window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                    }
                    case FULLSCREEN_BORDERLESS -> {
                        if (window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                        GLFW.glfwRestoreWindow(window.getWindow());
                        GLFW.glfwSetWindowAttrib(window.getWindow(),
                                GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                        Monitor monitor = window.findBestMonitor();
                        if (monitor != null) {
                            VideoMode videoMode = monitor.getCurrentMode();
                            int x = monitor.getX();
                            int y = monitor.getY();
                            int width = videoMode.getWidth();
                            int height = videoMode.getHeight();
                            GLFW.glfwSetWindowMonitor(window.getWindow(), MemoryUtil.NULL,
                                    x, y, width, height, GLFW.GLFW_DONT_CARE);
                        } else {
                            GLFW.glfwMaximizeWindow(window.getWindow());
                        }
                    }
                    case MAXIMIZED -> {
                        if (window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                        GLFW.glfwRestoreWindow(window.getWindow());
                        GLFW.glfwSetWindowAttrib(window.getWindow(),
                                GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                        GLFW.glfwMaximizeWindow(window.getWindow());
                    }
                    case MAXIMIZED_BORDERLESS -> {
                        if (window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                        GLFW.glfwRestoreWindow(window.getWindow());
                        GLFW.glfwSetWindowAttrib(window.getWindow(),
                                GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                        GLFW.glfwMaximizeWindow(window.getWindow());
                    }
                    case WINDOWED -> {
                        if (window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                        GLFW.glfwSetWindowAttrib(window.getWindow(),
                                GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                        GLFW.glfwRestoreWindow(window.getWindow());
                    }
                    case WINDOWED_BORDERLESS -> {
                        if (window.isFullscreen()) {
                            window.toggleFullScreen();
                        }
                        GLFW.glfwSetWindowAttrib(window.getWindow(),
                                GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                        GLFW.glfwRestoreWindow(window.getWindow());
                    }
                }
            }

            @Nonnull
            @Override
            public String toString() {
                return I18n.get("modernui.windowMode." + name().toLowerCase(Locale.ROOT));
            }
        }
    }



    public static class Text {

        public static final float BASE_FONT_SIZE_MIN = 6.5f;
        public static final float BASE_FONT_SIZE_MAX = 9.5f;
        public static final float BASELINE_MIN = 4;
        public static final float BASELINE_MAX = 10;
        public static final float SHADOW_OFFSET_MIN = 0.2f;
        public static final float SHADOW_OFFSET_MAX = 2;
        public static final float OUTLINE_OFFSET_MIN = 0.2f;
        public static final float OUTLINE_OFFSET_MAX = 2;
        public static final int LIFESPAN_MIN = 2;
        public static final int LIFESPAN_MAX = 15;


        //final boolean globalRenderer;
        public final boolean mAllowShadow;
        public final boolean mFixedResolution;
        public final double mBaseFontSize;
        public final double mBaselineShift;
        public final double mShadowOffset;
        public final double mOutlineOffset;
        //public final boolean mSuperSampling;
        //public final boolean mAlignPixels;
        public final int mCacheLifespan;
        //public final int mRehashThreshold;
        public final TextDirection mTextDirection;
        //public final boolean mBitmapReplacement;
        //public final boolean mUseDistanceField;
        //public final boolean mUseVanillaFont;
        public final boolean mUseTextShadersInWorld;
        public final DefaultFontBehavior mDefaultFontBehavior;
        public final List<String> mDefaultFontRuleSet;
        public final boolean mUseComponentCache;
        public final boolean mAllowAsyncLayout;
        public final LineBreakStyle mLineBreakStyle;
        public final LineBreakWordStyle mLineBreakWordStyle;
        public final boolean mSmartSDFShaders;
        public final boolean mComputeDeviceFontSize;
        public final boolean mAllowSDFTextIn2D;

        public final boolean mAntiAliasing;
        public final boolean mLinearMetrics;
        public final int mMinPixelDensityForSDF;
        //public final ModConfigSpec.BooleanValue mLinearSampling;

        //private final boolean antiAliasing;
        //private final boolean highPrecision;
        //private final boolean enableMipmap;
        //private final int mipmapLevel;
        //private final int resolutionLevel;
        //private final int defaultFontSize;
        

        void reload() {
            boolean reload = false;
            boolean reloadStrike = false;
            ModernTextRenderer.sAllowShadow = mAllowShadow;
            if (TextLayoutEngine.sFixedResolution != mFixedResolution) {
                TextLayoutEngine.sFixedResolution = mFixedResolution;
                reload = true;
            }
            if (TextLayoutProcessor.sBaseFontSize != mBaseFontSize) {
                TextLayoutProcessor.sBaseFontSize = (float) mBaseFontSize;
                reloadStrike = true;
            }
            TextLayout.sBaselineOffset = (float) mBaselineShift;
            ModernTextRenderer.sShadowOffset = (float) mShadowOffset;
            ModernTextRenderer.sOutlineOffset = (float) mOutlineOffset;

            TextLayoutEngine.sCacheLifespan = mCacheLifespan;
            if (TextLayoutEngine.sTextDirection != mTextDirection.key) {
                TextLayoutEngine.sTextDirection = mTextDirection.key;
                reload = true;
            }
            if (TextLayoutEngine.sDefaultFontBehavior != mDefaultFontBehavior.key) {
                TextLayoutEngine.sDefaultFontBehavior = mDefaultFontBehavior.key;
                reload = true;
            }
            List<? extends String> defaultFontRuleSet = mDefaultFontRuleSet;
            if (!Objects.equals(TextLayoutEngine.sDefaultFontRuleSet, defaultFontRuleSet)) {
                TextLayoutEngine.sDefaultFontRuleSet = defaultFontRuleSet;
                reload = true;
            }
            TextLayoutEngine.sRawUseTextShadersInWorld = mUseTextShadersInWorld;
            TextLayoutEngine.sUseComponentCache = mUseComponentCache;
            TextLayoutEngine.sAllowAsyncLayout = mAllowAsyncLayout;
            if (TextLayoutProcessor.sLbStyle != mLineBreakStyle.key) {
                TextLayoutProcessor.sLbStyle = mLineBreakStyle.key;
                reload = true;
            }
            if (TextLayoutProcessor.sLbWordStyle != mLineBreakWordStyle.key) {
                TextLayoutProcessor.sLbWordStyle = mLineBreakWordStyle.key;
                reload = true;
            }

            final boolean smartShaders = mSmartSDFShaders;
            Minecraft.getInstance().submit(() -> TextRenderType.toggleSDFShaders(smartShaders));

            ModernTextRenderer.sComputeDeviceFontSize = mComputeDeviceFontSize;
            ModernTextRenderer.sAllowSDFTextIn2D = mAllowSDFTextIn2D;

            if (GlyphManager.sAntiAliasing != mAntiAliasing) {
                GlyphManager.sAntiAliasing = mAntiAliasing;
                reloadStrike = true;
            }
            if (GlyphManager.sFractionalMetrics != mLinearMetrics) {
                GlyphManager.sFractionalMetrics = mLinearMetrics;
                reloadStrike = true;
            }
            if (TextLayoutEngine.sMinPixelDensityForSDF != mMinPixelDensityForSDF) {
                TextLayoutEngine.sMinPixelDensityForSDF = mMinPixelDensityForSDF;
                reload = true;
            }


            if (reloadStrike) {
                Minecraft.getInstance().submit(
                        () -> FontResourceManager.getInstance().reloadAll());
            } else if (reload && ModernUIMod.isTextEngineEnabled()) {
                Minecraft.getInstance().submit(
                        () -> {
                            try {
                                TextLayoutEngine.getInstance().reload();
                            } catch (Exception ignored) {
                            }
                        });
            }

            //GlyphManager.sResolutionLevel = resolutionLevel;
            //TextLayoutEngine.sDefaultFontSize = defaultFontSize;
        }

        public enum TextDirection {
            FIRST_STRONG(View.TEXT_DIRECTION_FIRST_STRONG, "FirstStrong"),
            ANY_RTL(View.TEXT_DIRECTION_ANY_RTL, "AnyRTL-LTR"),
            LTR(View.TEXT_DIRECTION_LTR, "LTR"),
            RTL(View.TEXT_DIRECTION_RTL, "RTL"),
            LOCALE(View.TEXT_DIRECTION_LOCALE, "Locale"),
            FIRST_STRONG_LTR(View.TEXT_DIRECTION_FIRST_STRONG_LTR, "FirstStrong-LTR"),
            FIRST_STRONG_RTL(View.TEXT_DIRECTION_FIRST_STRONG_RTL, "FirstStrong-RTL");

            private final int key;
            private final String text;

            TextDirection(int key, String text) {
                this.key = key;
                this.text = text;
            }

            @Override
            public String toString() {
                return text;
            }
        }

        public enum DefaultFontBehavior {
            IGNORE_ALL(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_IGNORE_ALL),
            KEEP_ASCII(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_KEEP_ASCII),
            KEEP_OTHER(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_KEEP_OTHER),
            KEEP_ALL(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_KEEP_ALL),
            ONLY_INCLUDE(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_ONLY_INCLUDE),
            ONLY_EXCLUDE(TextLayoutEngine.DEFAULT_FONT_BEHAVIOR_ONLY_EXCLUDE);

            private final int key;

            DefaultFontBehavior(int key) {
                this.key = key;
            }

            @Nonnull
            @Override
            public String toString() {
                return I18n.get("modernui.defaultFontBehavior." + name().toLowerCase(Locale.ROOT));
            }
        }

        public enum LineBreakStyle {
            AUTO(LineBreakConfig.LINE_BREAK_STYLE_NONE, "Auto"),
            LOOSE(LineBreakConfig.LINE_BREAK_STYLE_LOOSE, "Loose"),
            NORMAL(LineBreakConfig.LINE_BREAK_STYLE_NORMAL, "Normal"),
            STRICT(LineBreakConfig.LINE_BREAK_STYLE_STRICT, "Strict");

            private final int key;
            private final String text;

            LineBreakStyle(int key, String text) {
                this.key = key;
                this.text = text;
            }

            @Override
            public String toString() {
                return text;
            }
        }

        public enum LineBreakWordStyle {
            AUTO(LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE, "Auto"),
            PHRASE(LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE, "Phrase-based");

            private final int key;
            private final String text;

            LineBreakWordStyle(int key, String text) {
                this.key = key;
                this.text = text;
            }

            @Override
            public String toString() {
                return text;
            }
        }
    }

    // server config is available when integrated server or dedicated server started
    // if on dedicated server, all config data will sync to remote client via network
   */

}
