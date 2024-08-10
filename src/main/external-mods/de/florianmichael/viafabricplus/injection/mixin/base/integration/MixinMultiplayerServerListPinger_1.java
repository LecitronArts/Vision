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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.access.IClientConnection;
import de.florianmichael.viafabricplus.access.IServerInfo;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ServerStatusPinger$1")
public abstract class MixinMultiplayerServerListPinger_1 implements ClientStatusPacketListener {

    @Final
    @Shadow
    ServerData val$data;

    @Shadow
    @Final
    Connection val$connection;

    @Inject(method = "handleStatusResponse", at = @At("HEAD"))
    private void trackTranslatingState(ClientboundStatusResponsePacket packet, CallbackInfo ci) {
        // If ViaVersion is translating the current connection, we track the target version, and it's state in the server info
        // So we can later draw this information when hovering over the pingLegacyServer bar in the server list
        if (val$connection instanceof IClientConnection mixinClientConnection) {
            ((IServerInfo) val$data).viaFabricPlus$setTranslatingVersion(mixinClientConnection.viaFabricPlus$getTargetVersion());
        }
    }

    @Inject(method = "handleStatusResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V", shift = At.Shift.AFTER))
    private void fixVersionComparison(CallbackInfo ci) {
        final ProtocolVersion version = ((IClientConnection) this.val$connection).viaFabricPlus$getTargetVersion();

        // If the server is compatible with the client, we set the protocol version to the client version
        if (version != null && version.getVersion() == this.val$data.protocol) {
            this.val$data.protocol = SharedConstants.getProtocolVersion();
        }
    }

}
