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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.entity;

import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinBipedEntityModel<T extends LivingEntity> {

	@Shadow @Final public ModelPart rightArm;

	@Shadow @Final public ModelPart leftArm;

	@Inject(method = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/geom/ModelPart;zRot:F", ordinal = 1, shift = At.Shift.AFTER))
	private void addOldWalkAnimation(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
		if (VisualSettings.global().oldWalkingAnimation.isEnabled()) {
			this.rightArm.xRot = Mth.cos(f * 0.6662F + 3.1415927F) * 2.0F * g;
			this.rightArm.zRot = (Mth.cos(f * 0.2312F) + 1.0F) * 1.0F * g;

			this.leftArm.xRot = Mth.cos(f * 0.6662F) * 2.0F * g;
			this.leftArm.zRot = (Mth.cos(f * 0.2812F) - 1.0F) * 1.0F * g;
		}
	}

}
