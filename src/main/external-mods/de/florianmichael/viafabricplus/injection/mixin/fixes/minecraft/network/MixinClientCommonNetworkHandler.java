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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.storage.InventoryAcknowledgements;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.time.Duration;
import java.util.function.BooleanSupplier;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = ClientCommonPacketListenerImpl.class, priority = 1 /* Has to be applied before Fabric's Networking API, so it doesn't cancel our custom-payload packets */)
public abstract class MixinClientCommonNetworkHandler {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected abstract void sendWhen(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, Duration expiry);

    @Shadow
    public abstract void send(Packet<?> packet);

    @Shadow
    @Final
    protected Connection connection;

    @Shadow
    @Nullable
    private static URL parseResourcePackUrl(String url) {
        return null;
    }

    @Redirect(method = "handleKeepAlive", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientCommonPacketListenerImpl;sendWhen(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/BooleanSupplier;Ljava/time/Duration;)V"))
    private void forceSendKeepAlive(ClientCommonPacketListenerImpl instance, Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, Duration expiry) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            send(packet);
        } else {
            sendWhen(packet, sendCondition, expiry);
        }
    }

    @Inject(method = "handlePing", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onPing(ClientboundPingPacket packet, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            final InventoryAcknowledgements acks = (this.connection).viaFabricPlus$getUserConnection().get(InventoryAcknowledgements.class);
            if (acks.removeId(packet.getId())) {
                final short inventoryId = (short) ((packet.getId() >> 16) & 0xFF);

                AbstractContainerMenu handler = null;
                if (inventoryId == 0) handler = minecraft.player.inventoryMenu;
                else if (inventoryId == minecraft.player.containerMenu.containerId) handler = minecraft.player.containerMenu;

                if (handler != null) {
                    acks.addId(packet.getId());
                } else {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"), cancellable = true)
    private void handleSyncTask(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        /*
        if (packet.payload().id().toString().equals(ClientsideFixes.PACKET_SYNC_IDENTIFIER) && packet.payload() instanceof ResolvablePayload payload) {
            ClientsideFixes.handleSyncTask(((UntypedPayload) payload.resolve(null)).buffer());
            ci.cancel();
        }
        */
        if (packet.payload().id().toString().equals(ClientsideFixes.PACKET_SYNC_IDENTIFIER)) {
            ClientsideFixes.handleSyncTask((FriendlyByteBuf) packet.payload());
            ci.cancel(); // Cancel the packet, so it doesn't get processed by the client
        }
    }

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void validateUrlInNetworkThread(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            if (parseResourcePackUrl(packet.url()) == null) {
                this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.INVALID_URL));
                ci.cancel();
            }
        }
    }

}
