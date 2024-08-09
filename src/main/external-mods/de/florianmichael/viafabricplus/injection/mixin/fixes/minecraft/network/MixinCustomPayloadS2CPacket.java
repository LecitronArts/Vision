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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.network;

import com.google.common.collect.ImmutableMap;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.resources.ResourceLocation;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class MixinCustomPayloadS2CPacket {

    @Unique
    private static final Map<ResourceLocation, ProtocolVersion> viaFabricPlus$PAYLOAD_DIFF = ImmutableMap.<ResourceLocation, ProtocolVersion>builder()
            .put(BrandPayload.ID, LegacyProtocolVersion.c0_0_15a_1)
            .put(GameTestAddMarkerDebugPayload.ID, ProtocolVersion.v1_14)
            .put(GameTestClearMarkersDebugPayload.ID, ProtocolVersion.v1_14)
            .build();

    @Redirect(method = "readPayload", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", remap = false))
    private static Object filterAllowedCustomPayloads(Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> instance, Object object) {
        final ResourceLocation identifier = (ResourceLocation) object;
        if (instance.containsKey(identifier)) {
            final FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = instance.get(identifier);

            // Mods might add custom payloads that we don't want to filter, so we check for the namespace.
            // Mods should NEVER use the default namespace of the game, not only to not break this code,
            // but also to not break other mods and the game itself.
            if (!identifier.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                return reader;
            }

            // Technically it's wrong to just drop all payloads, but ViaVersion doesn't translate them and the server can't detect if
            // we handled the payload or not, so dropping them is easier than adding a bunch of useless translations for payloads
            // which doesn't do anything on the client anyway.
            if (!viaFabricPlus$PAYLOAD_DIFF.containsKey(identifier) || ProtocolTranslator.getTargetVersion().olderThan(viaFabricPlus$PAYLOAD_DIFF.get(identifier))) {
                return null;
            }
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20)) {
                // Skip remaining bytes after reading the payload and return null if the payload fails to read
                return (FriendlyByteBuf.Reader<? extends CustomPacketPayload>) packetByteBuf -> {
                    try {
                        final CustomPacketPayload result = reader.apply(packetByteBuf);
                        packetByteBuf.skipBytes(packetByteBuf.readableBytes());
                        return result;
                    } catch (Exception e) {
                        return null;
                    }
                };
            } else {
                return reader;
            }
        } else {
            return null;
        }
    }

}
