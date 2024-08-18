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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTabs.class)
public abstract class MixinItemGroups {

    @Shadow
    @Nullable
    private static CreativeModeTab.@Nullable ItemDisplayParameters CACHED_PARAMETERS;

    @Shadow
    private static void buildAllTabContents(CreativeModeTab.ItemDisplayParameters displayContext) {
    }

/*    @Unique
    private static ProtocolVersion viaFabricPlus$version;

    @Unique
    private static int viaFabricPlus$state;

    @Inject(method = "tryRebuildTabContents", at = @At("HEAD"), cancellable = true)
    private static void trackLastVersion(FeatureFlagSet enabledFeatures, boolean operatorEnabled, HolderLookup.Provider lookup, CallbackInfoReturnable<Boolean> cir) {
        if (viaFabricPlus$version != ProtocolTranslator.getTargetVersion() || viaFabricPlus$state != GeneralSettings.global().removeNotAvailableItemsFromCreativeTab.getIndex()) {
            viaFabricPlus$version = ProtocolTranslator.getTargetVersion();
            viaFabricPlus$state = GeneralSettings.global().removeNotAvailableItemsFromCreativeTab.getIndex();

            CACHED_PARAMETERS = new CreativeModeTab.ItemDisplayParameters(enabledFeatures, operatorEnabled, lookup);
            buildAllTabContents(CACHED_PARAMETERS);

            cir.setReturnValue(true);
        }
    }*/

}
