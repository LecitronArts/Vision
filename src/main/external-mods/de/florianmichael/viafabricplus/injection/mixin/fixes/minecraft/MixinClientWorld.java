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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft;

import de.florianmichael.viafabricplus.fixes.versioned.PendingUpdateManager1_18_2;
import de.florianmichael.viafabricplus.injection.access.IEntity;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(value = ClientLevel.class, priority = 900)
public abstract class MixinClientWorld extends Level {

    @Shadow
    @Final
    EntityTickList tickingEntities;

    @Mutable
    @Shadow
    @Final
    private BlockStatePredictionHandler blockStatePredictionHandler;

    protected MixinClientWorld(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, Supplier<ProfilerFiller> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void removePendingUpdateManager(ClientPacketListener networkHandler, ClientLevel.ClientLevelData properties, ResourceKey<Level> registryRef, Holder<DimensionType> dimensionTypeEntry, int loadDistance, int simulationDistance, Supplier<ProfilerFiller> profiler, LevelRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        if (DebugSettings.global().disableSequencing.isEnabled()) {
            this.blockStatePredictionHandler = new PendingUpdateManager1_18_2();
        }
    }

    /**
     * @author RK_01
     * @reason Versions <= 1.8.x and >= 1.17 always tick entities, even if they are not in a loaded chunk.
     */
    @Overwrite
    public void tickNonPassenger(Entity entity) {
        entity.setOldPosAndRot();
        final IEntity mixinEntity = (IEntity) entity;
        if (mixinEntity.viaFabricPlus$isInLoadedChunkAndShouldTick() || entity.isSpectator()) {
            entity.tickCount++;
            this.getProfiler().push(() -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            entity.tick();
            this.getProfiler().pop();
        }
        this.viaFabricPlus$checkChunk(entity);

        if (mixinEntity.viaFabricPlus$isInLoadedChunkAndShouldTick()) {
            for (Entity entity2 : entity.getPassengers()) {
                this.tickPassenger(entity, entity2);
            }
        }
    }

    /**
     * @author RK_01
     * @reason Versions <= 1.8.x and >= 1.17 always tick entities, even if they are not in a loaded chunk.
     */
    @Overwrite
    private void tickPassenger(Entity entity, Entity passenger) {
        if (!passenger.isRemoved() && passenger.getVehicle() == entity) {
            if (passenger instanceof Player || this.tickingEntities.contains(passenger)) {
                final IEntity mixinPassenger = (IEntity) passenger;
                passenger.setOldPosAndRot();
                if (mixinPassenger.viaFabricPlus$isInLoadedChunkAndShouldTick()) {
                    passenger.tickCount++;
                    passenger.rideTick();
                }
                this.viaFabricPlus$checkChunk(passenger);

                if (mixinPassenger.viaFabricPlus$isInLoadedChunkAndShouldTick()) {
                    for (Entity entity2 : passenger.getPassengers()) {
                        this.tickPassenger(passenger, entity2);
                    }
                }
            }
        } else {
            passenger.stopRiding();
        }
    }

    @Unique
    private void viaFabricPlus$checkChunk(Entity entity) {
        this.getProfiler().push("chunkCheck");
        final IEntity mixinEntity = (IEntity) entity;
        final int chunkX = Mth.floor(entity.getX() / 16.0D);
        final int chunkZ = Mth.floor(entity.getZ() / 16.0D);
        if (!mixinEntity.viaFabricPlus$isInLoadedChunkAndShouldTick() || entity.chunkPosition().x != chunkX || entity.chunkPosition().z != chunkZ) {
            if (!(this.getChunk(chunkX, chunkZ).isEmpty())) {
                mixinEntity.viaFabricPlus$setInLoadedChunkAndShouldTick(true);
            }
        }
        this.getProfiler().pop();
    }

}
