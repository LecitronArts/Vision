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

package de.florianmichael.viafabricplus.injection.mixin.fixes.authlib;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.fixes.versioned.KeyPairResponse1_19_0;
import de.florianmichael.viafabricplus.access.ILegacyKeySignatureStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;

@Mixin(value = YggdrasilUserApiService.class, remap = false)
public abstract class MixinYggdrasilUserApiService {

    @Shadow
    @Final
    private MinecraftClient minecraftClient;

    @Shadow
    @Final
    private URL routeKeyPair;

    @Inject(method = "getKeyPair", at = @At("HEAD"), cancellable = true)
    private void storeLegacyPublicKeySignature(CallbackInfoReturnable<KeyPairResponse> cir) {
        final KeyPairResponse1_19_0 response = minecraftClient.post(routeKeyPair, KeyPairResponse1_19_0.class);

        // the response can't be null for us since we are constructing a new object with it.
        if (response == null) {
            cir.setReturnValue(null);
            return;
        }

        // create the original KeyPairResponse object with the data
        final KeyPairResponse keyPairResponse = new KeyPairResponse(
                response.keyPair(),
                response.publicKeySignatureV2(),
                response.expiresAt(),
                response.refreshedAfter()
        );

        if (response.publicKeySignature() != null && response.publicKeySignature().array().length != 0) {
            ((ILegacyKeySignatureStorage) (Object) keyPairResponse).viafabricplus$setLegacyPublicKeySignature(response.publicKeySignature().array());
        } else {
            ViaFabricPlus.global().getLogger().error("Could not get legacy public key signature. 1.19.0 with secure-profiles enabled will not work!");
        }

        cir.setReturnValue(keyPairResponse);
    }

}
