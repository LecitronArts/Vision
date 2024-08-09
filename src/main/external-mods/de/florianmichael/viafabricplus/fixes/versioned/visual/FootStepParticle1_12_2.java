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

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FootStepParticle1_12_2 extends TextureSheetParticle {

    public static int ID;

    protected FootStepParticle1_12_2(ClientLevel clientWorld, double x, double y, double z) {
        super(clientWorld, x, y, z);

        this.quadSize = 0.125F;
        this.setLifetime(200);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        final float strength = ((float) this.age + tickDelta) / (float) this.lifetime;
        this.alpha = 2.0F - (strength * strength) * 2.0F;
        if (this.alpha > 1.0F) {
            this.alpha = 0.2F;
        } else {
            this.alpha *= 0.2F;
        }

        final Vec3 cameraPos = camera.getPosition();
        final float x = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPos.x());
        final float y = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPos.y());
        final float z = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPos.z());

        final float minU = this.getU0();
        final float maxU = this.getU1();
        final float minV = this.getV0();
        final float maxV = this.getV1();

        final int light = this.getLightColor(tickDelta); // This is missing in the original code, that's why the particles are broken
        vertexConsumer.vertex(x - quadSize, y, z + quadSize).uv(maxU, maxV).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(x + quadSize, y, z + quadSize).uv(maxU, minV).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(x + quadSize, y, z - quadSize).uv(minU, minV).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(x - quadSize, y, z - quadSize).uv(minU, maxV).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    public static void init() {
        final SimpleParticleType footStepType = FabricParticleTypes.simple(true);

        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("viafabricplus", "footstep"), footStepType);
        ParticleFactoryRegistry.getInstance().register(footStepType, FootStepParticle1_12_2.Factory::new);

        ID = BuiltInRegistries.PARTICLE_TYPE.getId(footStepType);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2)) {
                throw new UnsupportedOperationException("FootStepParticle is not supported on versions newer than 1.12.2");
            }

            final FootStepParticle1_12_2 particle = new FootStepParticle1_12_2(world, x, y, z);
            particle.pickSprite(this.spriteProvider);
            return particle;
        }

    }

}
