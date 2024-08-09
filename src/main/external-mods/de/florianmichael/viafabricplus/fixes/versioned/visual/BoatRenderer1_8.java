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

package de.florianmichael.viafabricplus.fixes.versioned.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Renderer for boats in 1.8 and lower.
 */
public class BoatRenderer1_8 extends EntityRenderer<Boat> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("viafabricplus", "textures/boat1_8.png");
    private final BoatModel1_8 model;

    public BoatRenderer1_8(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.5F;
        model = new BoatModel1_8(ctx.bakeLayer(BoatModel1_8.MODEL_LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return TEXTURE;
    }

    @Override
    public void render(Boat entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        matrices.pushPose();
        matrices.translate(0, 0.25, 0);
        matrices.mulPose(Axis.YP.rotationDegrees(180 - yaw));

        float damageWobbleTicks = entity.getHurtTime() - tickDelta;
        float damageWobbleStrength = entity.getDamage() - tickDelta;

        if (damageWobbleStrength < 0) {
            damageWobbleStrength = 0;
        }
        if (damageWobbleTicks > 0) {
            matrices.mulPose(Axis.XP.rotationDegrees(Mth.sin(damageWobbleTicks) * damageWobbleTicks * damageWobbleStrength / 10 * entity.getHurtDir()));
        }

        matrices.scale(-1, -1, 1);
        model.setupAnim(entity, tickDelta, 0, -0.1f, 0, 0);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        matrices.popPose();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

}
