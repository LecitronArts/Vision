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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;

@Mixin(PlayerInfo.class)
public abstract class MixinPlayerListEntry {

/*    @Redirect(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/SkinManager;getOrLoad(Lcom/mojang/authlib/GameProfile;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<PlayerSkin> fetchGameProfileProperties(SkinManager instance, GameProfile profile) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20) && !profile.getProperties().containsKey("textures")) {
            return CompletableFuture.supplyAsync(() -> {
                final ProfileResult profileResult = Minecraft.getInstance().getMinecraftSessionService().fetchProfile(profile.getId(), true);
                return profileResult == null ? profile : profileResult.profile();
            }, Util.backgroundExecutor()).thenCompose(instance::getOrLoad);
        } else {
            return instance.getOrLoad(profile);
        }
    }*/

}
