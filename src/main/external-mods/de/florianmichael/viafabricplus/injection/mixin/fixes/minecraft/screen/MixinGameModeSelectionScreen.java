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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.screen;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.network.chat.Component;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(GameModeSwitcherScreen.class)
public abstract class MixinGameModeSelectionScreen extends Screen {

    @Mutable
    @Shadow
    @Final
    private static int ALL_SLOTS_WIDTH;

    @Unique
    private GameModeSwitcherScreen.GameModeIcon[] viaFabricPlus$unwrappedGameModes;

    public MixinGameModeSelectionScreen(Component title) {
        super(title);
    }

/*    @Inject(method = "<init>", at = @At("RETURN"))
    private void fixUIWidth(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            final var gameModeSelections = new ArrayList<>(Arrays.stream(GameModeSwitcherScreen.GameModeIcon.values()).toList());

            gameModeSelections.remove(GameModeSwitcherScreen.GameModeIcon.SPECTATOR);
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
                gameModeSelections.remove(GameModeSwitcherScreen.GameModeIcon.ADVENTURE);
            }

            viaFabricPlus$unwrappedGameModes = gameModeSelections.toArray(GameModeSwitcherScreen.GameModeIcon[]::new);
            ALL_SLOTS_WIDTH = viaFabricPlus$unwrappedGameModes.length * 31 - 5;
        }
    }*/

/*
    @Redirect(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/debug/GameModeSwitcherScreen$GameModeIcon;VALUES:[Lnet/minecraft/client/gui/screens/debug/GameModeSwitcherScreen$GameModeIcon;"))
    private GameModeSwitcherScreen.GameModeIcon[] removeNewerGameModes() {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            return viaFabricPlus$unwrappedGameModes;
        } else {
            return GameModeSwitcherScreen.GameModeIcon.values();
        }
    }
*/

/*    @Inject(method = "init", at = @At("HEAD"))
    private void disableInClassic(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) { // survival mode was added in a1.0.15
            this.onClose();
        }
    }*/

}
