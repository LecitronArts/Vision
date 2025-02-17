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

package de.florianmichael.viafabricplus.injection.mixin.fixes.viaversion;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundPackets1_20_2;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Protocol1_20_2To1_20.class, remap = false)
public abstract class MixinProtocol1_20_2To1_20 {

/*    @Inject(method = "lambda$queueServerboundPacket$11", at = @At("HEAD"), cancellable = true)
    private static void dontQueueConfigPackets(ServerboundPackets1_20_2 packetType, PacketWrapper wrapper, CallbackInfo ci) {
        if (!DebugSettings.global().queueConfigPackets.getValue()) {
            ci.cancel();
            switch (packetType) {
                case PLUGIN_MESSAGE -> wrapper.setPacketType(ServerboundPackets1_19_4.PLUGIN_MESSAGE);
                case KEEP_ALIVE -> wrapper.setPacketType(ServerboundPackets1_19_4.KEEP_ALIVE);
                case PONG -> wrapper.setPacketType(ServerboundPackets1_19_4.PONG);
                default -> throw new IllegalStateException("Unexpected packet type: " + packetType);
            }
        }
    }*/

}
