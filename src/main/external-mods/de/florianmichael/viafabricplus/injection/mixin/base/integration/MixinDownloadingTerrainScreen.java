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

package de.florianmichael.viafabricplus.injection.mixin.base.integration;

import com.viaversion.viaversion.api.connection.UserConnection;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import de.florianmichael.viafabricplus.util.ChatUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.storage.ClassicProgressStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinDownloadingTerrainScreen extends Screen {

    public MixinDownloadingTerrainScreen(Component title) {
        super(title);
    }
/*    @Inject(method = "render", at = @At("RETURN"))
    private void renderClassicProgress(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (GeneralSettings.global().showClassicLoadingProgressInConnectScreen.getValue()) {
            // Check if ViaVersion is translating
            final UserConnection connection = ProtocolTranslator.getPlayNetworkUserConnection();
            if (connection == null) {
                return;
            }

            // Check if the client is connecting to a classic server
            final ClassicProgressStorage classicProgressStorage = connection.get(ClassicProgressStorage.class);
            if (classicProgressStorage == null) {
                return;
            }

            // Draw the classic loading progress
            context.drawCenteredString(
                    minecraft.font,
                    ChatUtil.prefixText(classicProgressStorage.status),
                    width / 2,
                    height / 2 - 30,
                    -1
            );
        }
    }*/

}
