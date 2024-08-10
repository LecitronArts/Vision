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

package de.florianmichael.viafabricplus.injection.mixin.base.perserverversion;

import com.llamalad7.mixinextras.sugar.Local;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.access.IPerformanceLog;
import de.florianmichael.viafabricplus.access.IServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.util.SampleLogger;

@Mixin(ServerStatusPinger.class)
public abstract class MixinMultiplayerServerListPinger {

    @Redirect(method = "pingServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddress;parseString(Ljava/lang/String;)Lnet/minecraft/client/multiplayer/resolver/ServerAddress;"))
    private ServerAddress replaceDefaultPort(String address, @Local(argsOnly = true) ServerData entry) {
        // Replace port when pinging the server and the forced version is set
        return ClientsideFixes.replaceDefaultPort(address, ((IServerInfo) entry).viaFabricPlus$forcedVersion());
    }

    @Redirect(method = "pingServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;connectToServer(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/SampleLogger;)Lnet/minecraft/network/Connection;"))
    private Connection setForcedVersion(InetSocketAddress address, boolean useEpoll, SampleLogger packetSizeLog, @Local(argsOnly = true) ServerData serverInfo) {
        final IServerInfo mixinServerInfo = (IServerInfo) serverInfo;

        if (mixinServerInfo.viaFabricPlus$forcedVersion() != null && !mixinServerInfo.viaFabricPlus$passedDirectConnectScreen()) {
            // We use the PerformanceLog field to store the forced version since it's always null when pinging a server
            // So we can create a dummy instance, store the forced version in it and later destroy the instance again
            // To avoid any side effects, we also support cases where a mod is also creating a PerformanceLog instance
            if (packetSizeLog == null) {
                packetSizeLog = new SampleLogger();
            }

            // Attach the forced version to the PerformanceLog instance
            ((IPerformanceLog) packetSizeLog).viaFabricPlus$setForcedVersion(mixinServerInfo.viaFabricPlus$forcedVersion());
            mixinServerInfo.viaFabricPlus$passDirectConnectScreen(false);
        }

        return Connection.connectToServer(address, useEpoll, packetSizeLog);
    }

}
