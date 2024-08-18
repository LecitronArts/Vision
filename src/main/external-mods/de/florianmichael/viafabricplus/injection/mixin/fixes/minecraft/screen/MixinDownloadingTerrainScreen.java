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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinDownloadingTerrainScreen extends Screen{

    @Shadow
    @Final
    private long createdAt;

    @Unique
    private int viaFabricPlus$tickCounter;

    @Unique
    private boolean viaFabricPlus$ready;

    @Unique
    private boolean viaFabricPlus$closeOnNextTick = false;

    public MixinDownloadingTerrainScreen(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void modifyCloseCondition(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            ci.cancel();

            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
                if (this.viaFabricPlus$ready) {
                    this.onClose();
                }

                if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_1)) {
                    this.viaFabricPlus$tickCounter++;
                    if (this.viaFabricPlus$tickCounter % 20 == 0) {
                        this.minecraft.getConnection().send(new ServerboundKeepAlivePacket(0));
                    }
                }
            } else {
                if (System.currentTimeMillis() > this.createdAt + 30000L) {
                    this.onClose();
                } else {
                    if (this.viaFabricPlus$closeOnNextTick) {
                        if (this.minecraft.player == null) return;

                        final BlockPos blockPos = this.minecraft.player.blockPosition();
                        final boolean isOutOfHeightLimit = this.minecraft.level != null && this.minecraft.level.isOutsideBuildHeight(blockPos.getY());
                        if (isOutOfHeightLimit || this.minecraft.levelRenderer.isSectionCompiled(blockPos) || this.minecraft.player.isSpectator() || !this.minecraft.player.isAlive()) {
                            this.onClose();
                        }
                    } else {
                        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1)) {
                            this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready || System.currentTimeMillis() > this.createdAt + 2000;
                        } else {
                            this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready;
                        }
                    }
                }
            }
        }
    }


    public void viaFabricPlus$setReady() {
        this.viaFabricPlus$ready = true;
    }

}
