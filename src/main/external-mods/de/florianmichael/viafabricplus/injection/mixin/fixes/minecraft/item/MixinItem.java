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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {

    @Shadow
    @Final
    private int maxDamage;

    @Shadow
    public abstract boolean isFood();

    @Redirect(method = {"getMaxDamage", "canBeDepleted", "getBarWidth", "getBarColor"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Item;maxDamage:I"))
    private int changeCrossbowDamage(Item instance) {
        if (instance instanceof ArmorItem armor && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            // Counterpart in MixinArmorMaterials
            return armor.getMaterial().getDurabilityForType(armor.getType());
        } else if (instance instanceof CrossbowItem && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_17_1)) {
            return 326;
        } else {
            return maxDamage;
        }
    }

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void dontStackFood(CallbackInfoReturnable<Integer> cir) {
        if (this.isFood() && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_7tob1_7_3)) {
            cir.setReturnValue(1);
        }
    }

    @ModifyExpressionValue(method = {"use", "finishUsingItem", "getUseAnimation", "getUseDuration"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;isEdible()Z"))
    private boolean makeFoodInstantConsumable(boolean original) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_7tob1_7_3)) {
            return false;
        } else {
            return original;
        }
    }

}
