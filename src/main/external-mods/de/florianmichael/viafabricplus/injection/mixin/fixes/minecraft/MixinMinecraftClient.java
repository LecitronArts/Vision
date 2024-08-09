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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import de.florianmichael.viafabricplus.fixes.data.ItemRegistryDiff;
import de.florianmichael.viafabricplus.injection.access.IMouseKeyboard;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    protected int missTime;

    @Shadow
    @Final
    public MouseHandler mouseHandler;

    @Shadow
    @Final
    public KeyboardHandler keyboardHandler;

    @Redirect(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setPickedItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void filterItem(Inventory instance, ItemStack stack) {
        if (ItemRegistryDiff.keepItem(stack.getItem())) {
            instance.setPickedItem(stack);
        }
    }

    /**
     * Never happens in Vanilla, this is only for {@link ItemRegistryDiff} to work
     */
    @Redirect(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleCreativeModeItemAdd(Lnet/minecraft/world/item/ItemStack;I)V"))
    private void dontSendEmptyItem(MultiPlayerGameMode instance, ItemStack stack, int slotId) {
        if (!stack.isEmpty()) {
            instance.handleCreativeModeItemAdd(stack, slotId);
        }
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionResult;shouldSwing()Z", ordinal = 0))
    private boolean disableSwing(InteractionResult instance) {
        return instance.shouldSwing() && ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15);
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionResult;shouldSwing()Z", ordinal = 2))
    private boolean disableSwing2(InteractionResult instance) {
        return instance.shouldSwing() && ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15);
    }

    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean disableSwing(LocalPlayer instance, InteractionHand hand) {
        return ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_15);
    }

    @Inject(method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0, shift = At.Shift.BEFORE),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 0),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V")
            )
    )
    private void processInputQueues(CallbackInfo ci) {
        if (DebugSettings.global().executeInputsSynchronously.isEnabled()) {
            Queue<Runnable> inputEvents = ((IMouseKeyboard) this.mouseHandler).viaFabricPlus$getPendingScreenEvents();
            while (!inputEvents.isEmpty()) inputEvents.poll().run();

            inputEvents = ((IMouseKeyboard) this.keyboardHandler).viaFabricPlus$getPendingScreenEvents();
            while (!inputEvents.isEmpty()) inputEvents.poll().run();
        }
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onOpenInventory()V", shift = At.Shift.AFTER))
    private void sendOpenInventoryPacket(CallbackInfo ci) throws Exception {
        if (DebugSettings.global().sendOpenInventoryPacket.isEnabled()) {
            final PacketWrapper clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, ProtocolTranslator.getPlayNetworkUserConnection());
            clientStatus.write(Type.VAR_INT, 2); // Open Inventory Achievement
            clientStatus.scheduleSendToServer(Protocol1_12To1_11_1.class);
        }
    }

    @Inject(method = "startAttack", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;", shift = At.Shift.BEFORE, ordinal = 0))
    private void fixSwingPacketOrder(CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            this.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @WrapWithCondition(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean fixSwingPacketOrder(LocalPlayer instance, InteractionHand hand) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_8);
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 1))
    private int moveCooldownIncrement(Minecraft instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return 0;
        } else {
            return missTime;
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V", shift = At.Shift.BEFORE))
    private void moveCooldownIncrement(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            if (this.missTime > 0) {
                --this.missTime;
            }
        }
    }

    @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean allowBlockBreakAndItemUsageAtTheSameTime(boolean original) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_7_6) && original;
    }

}
