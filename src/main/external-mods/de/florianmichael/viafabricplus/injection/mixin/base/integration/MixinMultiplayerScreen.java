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

import com.llamalad7.mixinextras.sugar.Local;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.access.IServerInfo;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {

    public MixinMultiplayerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addProtocolSelectionButton(CallbackInfo ci) {
        final int buttonPosition = GeneralSettings.global().multiplayerScreenButtonOrientation.getIndex();
        if (buttonPosition == 0) { // Off
            return;
        }
        Button.Builder builder = Button.builder(Component.literal("ViaFabricPlus"), button -> ProtocolSelectionScreen.INSTANCE.open(this)).size(98, 20);

        // Set the button's position according to the configured orientation and add the button to the screen
        this.addRenderableWidget(GeneralSettings.withOrientation(builder, buttonPosition, width, height).build());
    }

    @Redirect(method = "join", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddress;parseString(Ljava/lang/String;)Lnet/minecraft/client/multiplayer/resolver/ServerAddress;"))
    private ServerAddress replaceDefaultPort(String address, @Local(argsOnly = true) ServerData entry) {
        if (((IServerInfo) entry).viaFabricPlus$passedDirectConnectScreen()) {
            // If the user has already passed the direct connect screen, we use the target version
            return ClientsideFixes.replaceDefaultPort(address, ProtocolTranslator.getTargetVersion());
        } else {
            // Otherwise the forced version is used
            return ClientsideFixes.replaceDefaultPort(address, ((IServerInfo) entry).viaFabricPlus$forcedVersion());
        }
    }

}
