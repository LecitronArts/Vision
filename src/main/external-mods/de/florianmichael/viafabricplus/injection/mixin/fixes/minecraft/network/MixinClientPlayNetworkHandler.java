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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.data.recipe.RecipeInfo;
import de.florianmichael.viafabricplus.fixes.data.recipe.Recipes1_11_2;
import de.florianmichael.viafabricplus.injection.access.IDownloadingTerrainScreen;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonPacketListenerImpl {

    @Shadow
    public abstract void handleEntityEvent(ClientboundEntityEventPacket packet);

    @Mutable
    @Shadow
    @Final
    private Set<PlayerInfo> listedPlayers;

    @Shadow
    public abstract void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet);

    @Shadow
    public abstract Connection getConnection();

    @Shadow
    public abstract void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet);

    @Shadow
    protected abstract boolean enforcesSecureChat();

    protected MixinClientPlayNetworkHandler(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @WrapWithCondition(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;startWaitingForNewLevel(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
    private boolean checkDimensionChange(ClientPacketListener instance, LocalPlayer player, ClientLevel world, @Local ResourceKey<Level> registryKey) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_3) || registryKey != this.minecraft.player.level().dimension();
    }

    @WrapWithCondition(method = "handlePlayerChat", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", remap = false))
    private boolean removeChatPacketError(Logger instance, String s, Object o) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_2);
    }

    @Redirect(method = "applyPlayerInfoUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGameModeChanged(Lnet/minecraft/world/level/GameType;)V"))
    private void dontResetVelocity(LocalPlayer instance, GameType gameMode) {
        if (ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_20)) {
            instance.onGameModeChanged(gameMode);
        }
    }

    @WrapWithCondition(method = "initializeChatSession", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V", remap = false))
    private boolean removeInvalidSignatureWarning(Logger instance, String s, Object o) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_19_4);
    }

    @WrapWithCondition(method = "handlePlayerInfoUpdate", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private boolean removeUnknownPlayerListEntryWarning(Logger instance, String s, Object object1, Object object2) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_19_3);
    }

    @Redirect(method = {"handleTeleportEntity", "handleMoveEntity"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isControlledByLocalInstance()Z"))
    private boolean allowPlayerToBeMovedByEntityPackets(Entity instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3) || ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return instance.getControllingPassenger() instanceof Player player ? player.isLocalPlayer() : !instance.level().isClientSide;
        } else {
            return instance.isControlledByLocalInstance();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fixPlayerListOrdering(Minecraft client, Connection clientConnection, CommonListenerCookie clientConnectionState, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1)) {
            this.listedPlayers = new LinkedHashSet<>();
        }
    }

    @Redirect(method = "handleServerData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;enforcesSecureChat()Z"))
    private boolean removeSecureChatWarning(ClientPacketListener instance) {
        return enforcesSecureChat() || VisualSettings.global().disableSecureChatWarning.isEnabled();
    }

    @Inject(method = "handleSetSpawn", at = @At("RETURN"))
    private void moveDownloadingTerrainClosing(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_18_2, ProtocolVersion.v1_20_2) && this.minecraft.screen instanceof IDownloadingTerrainScreen mixinDownloadingTerrainScreen) {
            mixinDownloadingTerrainScreen.viaFabricPlus$setReady();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void closeDownloadingTerrain(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18) && this.minecraft.screen instanceof IDownloadingTerrainScreen mixinDownloadingTerrainScreen) {
            mixinDownloadingTerrainScreen.viaFabricPlus$setReady();
        }
    }

    @SuppressWarnings({"InvalidInjectorMethodSignature"})
    @ModifyConstant(method = "handleSetEntityPassengersPacket", constant = @Constant(classValue = Boat.class))
    private Class<?> dontChangeYawWhenMountingBoats(Object entity, Class<?> boatClass) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            return Integer.class; // Dummy class file to false the instanceof check
        } else {
            return boatClass;
        }
    }

    @Inject(method = "handleSetChunkCacheRadius", at = @At("RETURN"))
    private void emulateSimulationDistance(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_17_1)) {
            this.handleSetSimulationDistance(new ClientboundSetSimulationDistancePacket(packet.getRadius()));
        }
    }

    @Redirect(method = "handleTeleportEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFI)V"))
    private void cancelSmallChanges(Entity instance, double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_1) && Math.abs(instance.getX() - x) < 0.03125 && Math.abs(instance.getY() - y) < 0.015625 && Math.abs(instance.getZ() - z) < 0.03125) {
            instance.lerpTo(instance.getX(), instance.getY(), instance.getZ(), yaw, pitch, ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2) ? 0 : interpolationSteps);
        } else {
            instance.lerpTo(x, y, z, yaw, pitch, interpolationSteps);
        }
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void sendAdditionalData(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_11_1)) {
            final List<RecipeHolder<?>> recipes = new ArrayList<>();
            final List<RecipeInfo> recipeInfos = Recipes1_11_2.getRecipes(ProtocolTranslator.getTargetVersion());
            for (int i = 0; i < recipeInfos.size(); i++) {
                recipes.add(recipeInfos.get(i).create(new ResourceLocation("viafabricplus", "recipe/" + i)));
            }
            this.handleUpdateRecipes(new ClientboundUpdateRecipesPacket(recipes));

            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                this.handleEntityEvent(new ClientboundEntityEventPacket(this.minecraft.player, (byte) 28)); // Op-level 4
            }
        }
    }

}
