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

package de.florianmichael.viafabricplus.injection.mixin.viabedrock;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import de.florianmichael.viafabricplus.fixes.tracker.JoinGameDataTracker;
import net.raphimc.viabedrock.protocol.packet.JoinPackets;
import net.raphimc.viabedrock.protocol.types.primitive.LongLEType;
import net.raphimc.viabedrock.protocol.types.primitive.StringType;
import net.raphimc.viabedrock.protocol.types.primitive.VarIntType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = JoinPackets.class, remap = false)
public abstract class MixinJoinPackets {

    @Redirect(method = "lambda$register$8", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/packet/PacketWrapper;read(Lcom/viaversion/viaversion/api/type/Type;)Ljava/lang/Object;", ordinal = 5))
    private static Object trackWorldSeed(PacketWrapper instance, Type<LongLEType> tType) throws Exception {
        final Object seed = instance.read(tType);
        instance.user().get(JoinGameDataTracker.class).setSeed((long) seed);
        return seed;
    }

    @Redirect(method = "lambda$register$8", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/packet/PacketWrapper;read(Lcom/viaversion/viaversion/api/type/Type;)Ljava/lang/Object;", ordinal = 55))
    private static Object trackLevelId(PacketWrapper instance, Type<StringType> tType) throws Exception {
        final Object levelId = instance.read(tType);
        instance.user().get(JoinGameDataTracker.class).setLevelId((String) levelId);
        return levelId;
    }

    @Redirect(method = "lambda$register$8", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/packet/PacketWrapper;read(Lcom/viaversion/viaversion/api/type/Type;)Ljava/lang/Object;", ordinal = 63))
    private static Object trackEnchantmentSeed(PacketWrapper instance, Type<VarIntType> tType) throws Exception {
        final Object enchantmentSeed = instance.read(tType);
        instance.user().get(JoinGameDataTracker.class).setEnchantmentSeed((Integer) enchantmentSeed);
        return enchantmentSeed;
    }

}
