/*
 * This file is part of ViaLegacy - https://github.com/RaphiMC/ViaLegacy
 * Copyright (C) 2020-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import com.viaversion.viaversion.util.IdAndData;

public class ClassicBlockRemapper implements StorableObject {

    private final Int2ObjectFunction<IdAndData> mapper;
    private final Object2IntFunction<IdAndData> reverseMapper;

    public ClassicBlockRemapper(Int2ObjectFunction<IdAndData> mapper, Object2IntFunction<IdAndData> reverseMapper) {
        this.mapper = mapper;
        this.reverseMapper = reverseMapper;
    }

    public Int2ObjectFunction<IdAndData> getMapper() {
        return this.mapper;
    }

    public Object2IntFunction<IdAndData> getReverseMapper() {
        return this.reverseMapper;
    }

}
