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

import de.florianmichael.viafabricplus.injection.access.IRakSessionCodec;
import io.netty.util.collection.IntObjectMap;
import org.cloudburstmc.netty.channel.raknet.packet.EncapsulatedPacket;
import org.cloudburstmc.netty.channel.raknet.packet.RakDatagramPacket;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.netty.util.FastBinaryMinHeap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RakSessionCodec.class, remap = false)
public abstract class MixinRakSessionCodec implements IRakSessionCodec {

    @Shadow
    private FastBinaryMinHeap<EncapsulatedPacket> outgoingPackets;

    @Shadow
    private IntObjectMap<RakDatagramPacket> sentDatagrams;

    @Override
    public int viaFabricPlus$getOutgoingPackets() {
        return this.outgoingPackets.size();
    }

    @Override
    public int viaFabricPlus$SentDatagrams() {
        return this.sentDatagrams.size();
    }

}
