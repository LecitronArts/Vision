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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.injection.access.IServerInfo;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class MixinMultiplayerServerListWidget_ServerEntry {

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    protected abstract boolean isCompatible();

    @Mutable
    @Shadow
    @Final
    private FaviconTexture icon;

    @Unique
    private boolean viaFabricPlus$disableServerPinging = false;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ThreadPoolExecutor;submit(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;"))
    private boolean disableServerPinging(ThreadPoolExecutor instance, Runnable runnable) {
        ProtocolVersion version = ((IServerInfo) serverData).viaFabricPlus$forcedVersion();
        if (version == null) version = ProtocolTranslator.getTargetVersion();

        viaFabricPlus$disableServerPinging = VisualSettings.global().disableServerPinging.isEnabled(version);
        if (viaFabricPlus$disableServerPinging) {
            this.serverData.version = Component.nullToEmpty(version.getName()); // Show target version
        }
        return !viaFabricPlus$disableServerPinging;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList$OnlineServerEntry;isCompatible()Z"))
    private boolean disableServerPinging(ServerSelectionList.OnlineServerEntry instance) {
        if (viaFabricPlus$disableServerPinging) {
            return false; // server version will always been shown (as we don't have a player count anyway)
        } else {
            return isCompatible();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<FormattedCharSequence> disableServerPinging(Font instance, FormattedText text, int width) {
        if (viaFabricPlus$disableServerPinging) { // server label will just show the server address
            return instance.split(Component.nullToEmpty(serverData.ip), width);
        } else {
            return instance.split(text, width);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"), index = 2)
    private int disableServerPinging(int x) {
        if (viaFabricPlus$disableServerPinging) { // Move server label to the right (as we remove the pingLegacyServer bar)
            x += 15 /* pingLegacyServer bar width */ - 3 /* magical offset */;
        }
        return x;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0))
    private boolean disableServerPinging(GuiGraphics instance, ResourceLocation texture, int x, int y, int width, int height) {
        return !viaFabricPlus$disableServerPinging; // Remove pingLegacyServer bar
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen;setToolTip(Ljava/util/List;)V", ordinal = 1))
    private boolean disableServerPinging(JoinMultiplayerScreen instance, List<Component> tooltip) {
        return !viaFabricPlus$disableServerPinging; // Remove pingLegacyServer bar tooltip
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/FaviconTexture;textureLocation()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation disableServerPinging(FaviconTexture instance) {
        if (viaFabricPlus$disableServerPinging) { // Remove server icon
            return FaviconTexture.MISSING_LOCATION;
        } else {
            return this.icon.textureLocation();
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen;setToolTip(Ljava/util/List;)V", ordinal = 0))
    private void drawTranslatingState(JoinMultiplayerScreen instance, List<Component> tooltip, Operation<Void> original) {
        if (viaFabricPlus$disableServerPinging) { // Remove player list tooltip
            return;
        }
        final List<Component> tooltipCopy = new ArrayList<>(tooltip);
        if (GeneralSettings.global().showAdvertisedServerVersion.getValue()) {
            final ProtocolVersion version = ((IServerInfo) serverData).viaFabricPlus$translatingVersion();
            if (version != null) {
                tooltipCopy.add(Component.translatable("base.viafabricplus.via_translates_to", version.getName() + " (" + version.getOriginalVersion() + ")"));
                tooltipCopy.add(Component.translatable("base.viafabricplus.server_version", serverData.version.getString() + " (" + serverData.protocol + ")"));
            }
        }
        original.call(instance, tooltipCopy);
    }

}
