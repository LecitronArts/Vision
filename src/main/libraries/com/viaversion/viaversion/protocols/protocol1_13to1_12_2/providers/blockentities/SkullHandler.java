/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.blockentities;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.providers.BlockEntityProvider;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.storage.BlockStorage;

public class SkullHandler implements BlockEntityProvider.BlockEntityHandler {
    private static final int SKULL_WALL_START = 5447;
    private static final int SKULL_END = 5566;

    @Override
    public int transform(UserConnection user, CompoundTag tag) {
        BlockStorage storage = user.get(BlockStorage.class);
        Position position = new Position(tag.getNumberTag("x").asInt(), tag.getNumberTag("y").asShort(), tag.getNumberTag("z").asInt());

        if (!storage.contains(position)) {
            Via.getPlatform().getLogger().warning("Received an head update packet, but there is no head! O_o " + tag);
            return -1;
        }

        int id = storage.get(position).getOriginal();
        if (id >= SKULL_WALL_START && id <= SKULL_END) {
            NumberTag skullType = tag.getNumberTag("SkullType");
            if (skullType != null) {
                id += skullType.asInt() * 20;
            }

            NumberTag rot = tag.getNumberTag("Rot");
            if (rot != null) {
                id += rot.asInt();
            }
        } else {
            Via.getPlatform().getLogger().warning("Why does this block have the skull block entity? " + tag);
            return -1;
        }

        return id;
    }
}
