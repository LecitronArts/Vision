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

package de.florianmichael.viafabricplus.settings.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.settings.base.BooleanSetting;
import de.florianmichael.viafabricplus.settings.base.ButtonSetting;
import de.florianmichael.viafabricplus.settings.base.SettingGroup;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

public class BedrockSettings extends SettingGroup {

    private static final BedrockSettings INSTANCE = new BedrockSettings();

    private final ButtonSetting ignored = new ButtonSetting(this, Component.translatable("bedrock_settings.viafabricplus.click_to_set_bedrock_account"), () -> CompletableFuture.runAsync(this::openBedrockAccountLogin)) {
        
        @Override
        public MutableComponent displayValue() {
            final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getBedrockAccount();
            if (account != null) {
                return Component.literal("Bedrock account: " + account.getMcChain().getDisplayName());
            } else {
                return super.displayValue();
            }
        }
    };
    public final BooleanSetting openPromptGUIToConfirmTransfer = new BooleanSetting(this, Component.translatable("bedrock_settings.viafabricplus.confirm_transfer_server_prompt"), true);
    public final BooleanSetting replaceDefaultPort = new BooleanSetting(this, Component.translatable("bedrock_settings.viafabricplus.replace_default_port"), true);

    public BedrockSettings() {
        super(Component.translatable("setting_group_name.viafabricplus.bedrock"));
    }
    
    private void openBedrockAccountLogin() {
        final Minecraft client = Minecraft.getInstance();
        final Screen prevScreen = client.screen;
        try {
            ViaFabricPlus.global().getSaveManager().getAccountsSave().setBedrockAccount(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                client.execute(() -> client.setScreen(new AlertScreen(() -> {
                    client.setScreen(prevScreen);
                    Thread.currentThread().interrupt();
                }, Component.literal("Microsoft Bedrock login"), Component.translatable("bedrock.viafabricplus.login"), Component.translatable("base.viafabricplus.cancel"), true)));
                try {
                    Util.getPlatform().openUri(new URI(msaDeviceCode.getDirectVerificationUri()));
                } catch (URISyntaxException e) {
                    Thread.currentThread().interrupt();
                    VFPScreen.showErrorScreen("Microsoft Bedrock Login", e, prevScreen);
                }
            })));

            RenderSystem.recordRenderCall(() -> client.setScreen(prevScreen));
        } catch (Throwable e) {
            Thread.currentThread().interrupt();
            VFPScreen.showErrorScreen("Microsoft Bedrock Login", e, prevScreen);
        }
    }

    public static BedrockSettings global() {
        return INSTANCE;
    }

}
