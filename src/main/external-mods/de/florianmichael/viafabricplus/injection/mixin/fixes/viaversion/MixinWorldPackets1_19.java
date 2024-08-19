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

import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.packets.WorldPackets;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.protocoltranslator.translator.BlockStateTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldPackets.class, remap = false)
public abstract class MixinWorldPackets1_19 {

/*    @Redirect(method = "register", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/protocols/protocol1_19to1_18_2/Protocol1_19To1_18_2;cancelClientbound(Lcom/viaversion/viaversion/api/protocol/packet/ClientboundPacketType;)V"))
    private static void handleLegacyAcknowledgePlayerDigging(Protocol1_19To1_18_2 instance, ClientboundPacketType clientboundPacketType) {
        instance.registerClientbound(ClientboundPackets1_18.ACKNOWLEDGE_PLAYER_DIGGING, ClientboundPackets1_19.PLUGIN_MESSAGE, wrapper -> {
            wrapper.resetReader();

            final String uuid = ClientsideFixes.executeSyncTask(data -> {
                try {
                    final BlockPos pos = data.readBlockPos();
                    final BlockState blockState = BlockStateTranslator.via1_18_2toMc(data.readVarInt());
                    final ServerboundPlayerActionPacket.Action action = data.readEnum(ServerboundPlayerActionPacket.Action.class);
                    final boolean allGood = data.readBoolean();

*//*                    final var mixinInteractionManager = (IClientPlayerInteractionManager) Minecraft.getInstance().gameMode;*//*
                    Minecraft.getInstance().gameMode.viaFabricPlus$get1_18_2InteractionManager().handleBlockBreakAck(pos, blockState, action, allGood);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to handle BlockBreakAck packet data", t);
                }
            });
            wrapper.write(Type.STRING, ClientsideFixes.PACKET_SYNC_IDENTIFIER);
            wrapper.write(Type.STRING, uuid);
        });
    }*/

}
