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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Options.class)
public abstract class MixinGameOptions {

    @Shadow
    public boolean useNativeTransport;

    @ModifyVariable(method = "setServerRenderDistance", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int changeServerViewDistance(int viewDistance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_17_1)) {
            return 0;
        } else {
            return viewDistance;
        }
    }

    /**
     * @author RK_01
     * @reason Needed as an indicator if the client wants to pingLegacyServer a server or connect to a server
     */
    @Overwrite
    public boolean useNativeTransport() {
        if (!this.useNativeTransport) {
            ViaFabricPlus.global().getLogger().error("Native transport is disabled, but enabling it anyway since we use it as an indicator if the client wants to pingLegacyServer a server or connect to a server.");
        }
        return true;
    }

}
