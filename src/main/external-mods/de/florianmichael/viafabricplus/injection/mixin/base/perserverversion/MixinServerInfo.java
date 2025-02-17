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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerData.class)
public abstract class MixinServerInfo  {

/*    @Shadow
    public String name;

    @Unique
    private ProtocolVersion viaFabricPlus$forcedVersion = null;

    @Unique
    private boolean viaFabricPlus$passedDirectConnectScreen;

    @Unique
    private ProtocolVersion viaFabricPlus$translatingVersion;

    @Inject(method = "write", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void saveForcedVersion(CallbackInfoReturnable<CompoundTag> cir, CompoundTag nbtCompound) {
        if (viaFabricPlus$forcedVersion != null) {
            nbtCompound.putString("viafabricplus_forcedversion", viaFabricPlus$forcedVersion.getName());
        }
    }

    @Inject(method = "read", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void loadForcedVersion(CompoundTag root, CallbackInfoReturnable<ServerData> cir, ServerData serverInfo) {
        if (root.contains("viafabricplus_forcedversion")) {
            final ProtocolVersion version = ProtocolVersion.getClosest(root.getString("viafabricplus_forcedversion"));
            if (version != null) {
                ( serverInfo).viaFabricPlus$forceVersion(version);
            }
        }
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void syncForcedVersion(ServerData serverInfo, CallbackInfo ci) {
        viaFabricPlus$forceVersion(((IServerInfo) serverInfo).viaFabricPlus$forcedVersion());
    }

    @Override
    public ProtocolVersion viaFabricPlus$forcedVersion() {
        return viaFabricPlus$forcedVersion;
    }

    @Override
    public void viaFabricPlus$forceVersion(ProtocolVersion version) {
        viaFabricPlus$forcedVersion = version;
    }

    @Override
    public boolean viaFabricPlus$passedDirectConnectScreen() {
        return viaFabricPlus$passedDirectConnectScreen;
    }

    @Override
    public void viaFabricPlus$passDirectConnectScreen(boolean state) {
        viaFabricPlus$passedDirectConnectScreen = state;
    }

    @Override
    public ProtocolVersion viaFabricPlus$translatingVersion() {
        return viaFabricPlus$translatingVersion;
    }

    @Override
    public void viaFabricPlus$setTranslatingVersion(ProtocolVersion version) {
        viaFabricPlus$translatingVersion = version;
    }*/

}
