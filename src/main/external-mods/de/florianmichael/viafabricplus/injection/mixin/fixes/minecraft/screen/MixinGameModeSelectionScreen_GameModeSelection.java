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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("DataFlowIssue")
@Mixin(GameModeSwitcherScreen.GameModeIcon.class)
public abstract class MixinGameModeSelectionScreen_GameModeSelection {

    @Shadow
    @Final
    public static GameModeSwitcherScreen.GameModeIcon SURVIVAL;

    @Shadow
    @Final
    public static GameModeSwitcherScreen.GameModeIcon CREATIVE;

    @Inject(method = "getNext", at = @At("HEAD"), cancellable = true)
    private void unwrapGameModes(CallbackInfoReturnable<GameModeSwitcherScreen.GameModeIcon> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            switch ((GameModeSwitcherScreen.GameModeIcon) (Object) this) {
                case CREATIVE -> cir.setReturnValue(SURVIVAL);
                case SURVIVAL -> {
                    if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
                        cir.setReturnValue(CREATIVE);
                    } else {
                        cir.setReturnValue(GameModeSwitcherScreen.GameModeIcon.ADVENTURE);
                    }
                }
                case ADVENTURE -> cir.setReturnValue(CREATIVE);
            }
        }
    }

    @Inject(method = "getCommand", at = @At("HEAD"), cancellable = true)
    private void oldCommand(CallbackInfoReturnable<String> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
            cir.setReturnValue(
                    "gamemode " + Minecraft.getInstance().getUser().getName() + ' ' + switch (((Enum<?>) (Object) this).ordinal()) {
                        case 0, 3 -> 1;
                        case 1, 2 -> 0;
                        default -> throw new AssertionError();
                    }
            );
        }
    }

}
