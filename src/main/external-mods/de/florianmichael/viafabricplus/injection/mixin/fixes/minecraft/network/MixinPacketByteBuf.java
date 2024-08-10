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
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.access.IItemStack;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FriendlyByteBuf.class)
public abstract class MixinPacketByteBuf {

    @Redirect(method = "readItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setTag(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void removeViaFabricPlusTag(ItemStack instance, CompoundTag tag) {
        if (tag != null && tag.contains(ClientsideFixes.ITEM_COUNT_NBT_TAG, Tag.TAG_BYTE) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
            final IItemStack mixinItemStack = ((IItemStack) (Object) instance);
            mixinItemStack.viaFabricPlus$set1_10Count(tag.getByte(ClientsideFixes.ITEM_COUNT_NBT_TAG));
            tag.remove(ClientsideFixes.ITEM_COUNT_NBT_TAG);
            if (tag.isEmpty()) tag = null;
        }

        instance.setTag(tag);
    }

    @Redirect(method = "writeItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getTag()Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag addViaFabricPlusTag(ItemStack instance) {
        CompoundTag tag = instance.getTag();

        final IItemStack mixinItemStack = ((IItemStack) (Object) instance);
        if (mixinItemStack.viaFabricPlus$has1_10Tag()) {
            if (tag == null) tag = new CompoundTag();
            tag.putByte(ClientsideFixes.ITEM_COUNT_NBT_TAG, (byte) mixinItemStack.viaFabricPlus$get1_10Count());
        }

        return tag;
    }

}
