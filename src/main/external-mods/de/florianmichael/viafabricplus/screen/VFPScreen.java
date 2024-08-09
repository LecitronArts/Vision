/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * This class is a wrapper for the {@link net.minecraft.client.gui.screens.Screen} class which provides some global
 * functions and features used in all screens which are added by ViaFabricPlus
 */
public class VFPScreen extends Screen {

    private static final String MOD_URL = "https://github.com/ViaVersion/ViaFabricPlus";

    private final boolean backButton;
    public Screen prevScreen;

    private Component subtitle;
    private Button.OnPress subtitlePressAction;

    private PlainTextButton subtitleWidget;

    public VFPScreen(final String title, final boolean backButton) {
        super(Component.nullToEmpty(title));
        this.backButton = backButton;
    }

    /**
     * Sets the subtitle and the subtitle press action to the default values
     * The default value of the subtitle is the url to the GitHub repository of VFP
     * The default value of the subtitle press action is to open the url in a confirmation screen
     */
    public void setupDefaultSubtitle() {
        this.setupUrlSubtitle(MOD_URL);
    }

    /**
     * Sets the subtitle and the subtitle press action to the default values
     *
     * @param subtitle The subtitle which should be rendered
     */
    public void setupUrlSubtitle(final String subtitle) {
        this.setupSubtitle(Component.nullToEmpty(subtitle), ConfirmLinkScreen.confirmLink(this, subtitle));
    }


    /**
     * Sets the subtitle and the subtitle press action
     *
     * @param subtitle The subtitle which should be rendered
     */
    public void setupSubtitle(@Nullable final Component subtitle) {
        this.setupSubtitle(subtitle, null);
    }

    /**
     * Sets the subtitle and the subtitle press action
     *
     * @param subtitle The subtitle which should be rendered
     * @param subtitlePressAction The press action which should be executed when the subtitle is clicked
     */
    public void setupSubtitle(@Nullable final Component subtitle, @Nullable final Button.OnPress subtitlePressAction) {
        this.subtitle = subtitle;
        this.subtitlePressAction = subtitlePressAction;

        if (subtitleWidget != null) { // Allows to remove the subtitle when calling this method twice.
            removeWidget(subtitleWidget);
            subtitleWidget = null;
        }
        if (subtitlePressAction != null) {
            final int subtitleWidth = font.width(subtitle);
            this.addRenderableWidget(subtitleWidget = new PlainTextButton(width / 2 - (subtitleWidth / 2), (font.lineHeight + 2) * 2 + 3, subtitleWidth, font.lineHeight + 2, subtitle, subtitlePressAction, font));
        }
    }

    /**
     * Intended method to open a VFP screen
     *
     * @param prevScreen The current screen from which the VFP screen is opened
     */
    public void open(final Screen prevScreen) {
        this.prevScreen = prevScreen;

        RenderSystem.recordRenderCall(() -> Minecraft.getInstance().setScreen(this));
    }

    @Override
    protected void init() {
        if (backButton) {
            this.addRenderableWidget(Button.builder(Component.literal("<-"), button -> this.onClose()).pos(5, 5).size(20, 20).build());
        }
    }

    @Override
    public void onClose() {
        if (prevScreen instanceof VFPScreen vfpScreen) {
            vfpScreen.open(vfpScreen.prevScreen); // Support recursive opening
        } else {
            Minecraft.getInstance().setScreen(prevScreen);
        }
    }

    /**
     * Renders the ViaFabricPlus title
     *
     * @param context The current draw context
     */
    public void renderTitle(final GuiGraphics context) {
        final PoseStack matrices = context.pose();

        matrices.pushPose();
        matrices.scale(2F, 2F, 2F);
        context.drawCenteredString(font, "ViaFabricPlus", width / 4, 3, Color.ORANGE.getRGB());
        matrices.popPose();

        renderSubtitle(context);
    }

    /**
     * Renders the subtitle that doesn't have a press action
     *
     * @param context The current draw context
     */
    public void renderSubtitle(final GuiGraphics context) {
        if (subtitle != null && subtitlePressAction == null) {
            context.drawCenteredString(font, subtitle, width / 2, (font.lineHeight + 2) * 2 + 3, -1);
        }
    }

    /**
     * Plays Minecraft's button click sound
     */
    public static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /**
     * Opens an error screen with a specific title and throws the given throwable
     *
     * @param title     The title of the error screen
     * @param throwable The throwable which should be thrown
     * @param next      The screen which should be opened after the error screen is closed
     */
    public static void showErrorScreen(final String title, final Throwable throwable, final Screen next) {
        ViaFabricPlus.global().getLogger().error("Something went wrong!", throwable);

        final Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new AlertScreen(() -> client.setScreen(next), Component.nullToEmpty(title), Component.translatable("base.viafabricplus.something_went_wrong").append("\n" + throwable.getMessage()), Component.translatable("base.viafabricplus.cancel"), false)));
    }

}
