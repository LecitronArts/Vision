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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.protocoltranslator.impl.provider.vialegacy.ViaFabricPlusClassicMPPassProvider;
import de.florianmichael.viafabricplus.protocoltranslator.util.ProtocolVersionDetector;
import de.florianmichael.viafabricplus.settings.impl.AuthenticationSettings;
import io.netty.channel.ChannelFuture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public abstract class MixinConnectScreen_1 {
/*
    @Shadow
    @Final
    ServerData val$pServerData;

    @Shadow
    @Final
    ConnectScreen this$0;

    @Unique
    private boolean viaFabricPlus$useClassiCubeAccount;

    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture setServerInfoAndHandleDisconnect(InetSocketAddress address, boolean useEpoll, Connection connection, Operation<ChannelFuture> original) {
        final IServerInfo mixinServerInfo = (IServerInfo) this.val$pServerData;

        ProtocolVersion targetVersion = ProtocolTranslator.getTargetVersion();
        if (mixinServerInfo.viaFabricPlus$forcedVersion() != null && !mixinServerInfo.viaFabricPlus$passedDirectConnectScreen()) {
            targetVersion = mixinServerInfo.viaFabricPlus$forcedVersion();
            mixinServerInfo.viaFabricPlus$passDirectConnectScreen(false); // reset state
        }
        if (targetVersion == ProtocolTranslator.AUTO_DETECT_PROTOCOL) {
            this.this$0.updateStatus(Component.translatable("base.viafabricplus.detecting_server_version"));
            targetVersion = ProtocolVersionDetector.get(address, ProtocolTranslator.NATIVE_VERSION);
        }
        ProtocolTranslator.setTargetVersion(targetVersion, true);

        this.viaFabricPlus$useClassiCubeAccount = AuthenticationSettings.global().setSessionNameToClassiCubeNameInServerList.getValue() && ViaFabricPlusClassicMPPassProvider.classicMpPassForNextJoin != null;

        final ChannelFuture future = original.call(address, useEpoll, connection);
        ProtocolTranslator.injectPreviousVersionReset(future.channel());

        return future;
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/User;getName()Ljava/lang/String;"))
    private String useClassiCubeUsername(User instance) {
        if (this.viaFabricPlus$useClassiCubeAccount) {
            final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount();
            if (account != null) return account.username();
        }
        return instance.getName();
    }*/

}
