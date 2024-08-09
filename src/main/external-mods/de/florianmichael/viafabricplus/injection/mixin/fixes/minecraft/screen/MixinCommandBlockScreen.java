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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.screen;

import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockEditScreen.class)
public abstract class MixinCommandBlockScreen {

    @Shadow
    private CycleButton<CommandBlockEntity.Mode> modeButton;

    @Shadow
    private CycleButton<Boolean> conditionalModeButton;

    @Shadow
    private CycleButton<Boolean> redstoneTriggerButton;

    @Shadow
    public abstract void updateCommandBlock();

    @Inject(method = "init", at = @At("TAIL"))
    private void removeWidgets(CallbackInfo ci) {
        if (VisualSettings.global().removeNewerFeaturesFromCommandBlockScreen.isEnabled()) {
            modeButton.visible = false;
            conditionalModeButton.visible = false;
            redstoneTriggerButton.visible = false;

            updateCommandBlock();
        }
    }

}
