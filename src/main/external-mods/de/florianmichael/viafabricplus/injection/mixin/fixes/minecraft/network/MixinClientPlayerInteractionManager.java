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
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import de.florianmichael.viafabricplus.fixes.versioned.ActionResultException1_12_2;
import de.florianmichael.viafabricplus.fixes.versioned.ClientPlayerInteractionManager1_18_2;
import de.florianmichael.viafabricplus.injection.access.IClientConnection;
import de.florianmichael.viafabricplus.injection.access.IClientPlayerInteractionManager;
import de.florianmichael.viafabricplus.injection.access.IScreenHandler;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.protocoltranslator.impl.provider.viaversion.ViaFabricPlusHandItemProvider;
import de.florianmichael.viafabricplus.protocoltranslator.translator.ItemTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@SuppressWarnings("DataFlowIssue")
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinClientPlayerInteractionManager implements IClientPlayerInteractionManager {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract InteractionResult performUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult);

    @Shadow
    @Final
    private ClientPacketListener connection;

    @Shadow
    private BlockPos destroyBlockPos;

    @Shadow
    private float destroyProgress;

    @Shadow
    protected abstract void startPrediction(ClientLevel world, PredictiveAction packetCreator);

    @Shadow
    private GameType localPlayerMode;

    @Unique
    private ItemStack viaFabricPlus$oldCursorStack;

    @Unique
    private List<ItemStack> viaFabricPlus$oldItems;

    @Unique
    private final ClientPlayerInteractionManager1_18_2 viaFabricPlus$1_18_2InteractionManager = new ClientPlayerInteractionManager1_18_2();

    @Inject(method = "getDestroyStage", at = @At("HEAD"), cancellable = true)
    private void changeCalculation(CallbackInfoReturnable<Integer> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
            cir.setReturnValue((int) (this.destroyProgress * 10.0F) - 1);
        }
    }

    @Inject(method = "startPrediction", at = @At("HEAD"))
    private void trackPlayerAction(ClientLevel world, PredictiveAction packetCreator, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2) && packetCreator instanceof ServerboundPlayerActionPacket playerActionC2SPacket) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(playerActionC2SPacket.getAction(), playerActionC2SPacket.getPos());
        }
    }

    @Redirect(method = {"startDestroyBlock", "stopDestroyBlock"}, at = @At(value = "NEW", target = "(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;"))
    private ServerboundPlayerActionPacket trackPlayerAction(ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction direction) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
        }
        return new ServerboundPlayerActionPacket(action, pos, direction);
    }

    @WrapWithCondition(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    private boolean redirectPlayerPosPacket(ClientPacketListener instance, Packet<?> packet) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_16_4);
    }

    @ModifyVariable(method = "handleInventoryMouseClick", at = @At(value = "STORE"), ordinal = 0)
    private List<ItemStack> captureOldItems(List<ItemStack> oldItems) {
        viaFabricPlus$oldCursorStack = minecraft.player.containerMenu.getCarried().copy();
        return this.viaFabricPlus$oldItems = oldItems;
    }

    @WrapWithCondition(method = "handleInventoryMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private boolean handleWindowClick1_16_5(ClientPacketListener instance, Packet<?> packet) throws Exception {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4) && packet instanceof ServerboundContainerClickPacket clickSlot) {
            ItemStack slotItemBeforeModification;
            if (this.viaFabricPlus$shouldBeEmpty(clickSlot.getClickType(), clickSlot.getSlotNum())) {
                slotItemBeforeModification = ItemStack.EMPTY;
            } else if (clickSlot.getSlotNum() < 0 || clickSlot.getSlotNum() >= viaFabricPlus$oldItems.size()) {
                slotItemBeforeModification = viaFabricPlus$oldCursorStack;
            } else {
                slotItemBeforeModification = viaFabricPlus$oldItems.get(clickSlot.getSlotNum());
            }

            final PacketWrapper clickWindowPacket = PacketWrapper.create(ServerboundPackets1_16_2.CLICK_WINDOW, ((IClientConnection) connection.getConnection()).viaFabricPlus$getUserConnection());
            clickWindowPacket.write(Type.UNSIGNED_BYTE, (short) clickSlot.getContainerId());
            clickWindowPacket.write(Type.SHORT, (short) clickSlot.getSlotNum());
            clickWindowPacket.write(Type.BYTE, (byte) clickSlot.getButtonNum());
            clickWindowPacket.write(Type.SHORT, ((IScreenHandler) minecraft.player.containerMenu).viaFabricPlus$incrementAndGetActionId());
            clickWindowPacket.write(Type.VAR_INT, clickSlot.getClickType().ordinal());
            clickWindowPacket.write(Type.ITEM1_13_2, ItemTranslator.mcToVia(slotItemBeforeModification, ProtocolVersion.v1_16_4));
            clickWindowPacket.scheduleSendToServer(Protocol1_17To1_16_4.class);

            viaFabricPlus$oldCursorStack = null;
            viaFabricPlus$oldItems = null;
            return false;
        }

        return true;
    }

    @Redirect(method = {"lambda$startDestroyBlock$0", "lambda$continueDestroyBlock$2"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean checkFireBlock(MultiPlayerGameMode instance, BlockPos pos, @Local Direction direction) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return !this.viaFabricPlus$extinguishFire(pos, direction) && instance.destroyBlock(pos);
        } else {
            return instance.destroyBlock(pos);
        }
    }

    @Inject(method = "destroyBlock", at = @At("TAIL"))
    private void resetBlockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
            this.destroyBlockPos = new BlockPos(this.destroyBlockPos.getX(), -1, this.destroyBlockPos.getZ());
        }
    }

    @Inject(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 2, shift = At.Shift.BEFORE))
    private void interactBlock1_12_2(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            final ItemStack itemStack = player.getItemInHand(hand);
            BlockHitResult checkHitResult = hitResult;
            if (itemStack.getItem() instanceof BlockItem) {
                final BlockState clickedBlock = this.minecraft.level.getBlockState(hitResult.getBlockPos());
                if (clickedBlock.getBlock().equals(Blocks.SNOW)) {
                    if (clickedBlock.getValue(SnowLayerBlock.LAYERS) == 1) {
                        checkHitResult = hitResult.withDirection(Direction.UP);
                    }
                }
                final UseOnContext itemUsageContext = new UseOnContext(player, hand, checkHitResult);
                final BlockPlaceContext itemPlacementContext = new BlockPlaceContext(itemUsageContext);
                if (!itemPlacementContext.canPlace() || ((BlockItem) itemPlacementContext.getItemInHand().getItem()).getPlacementState(itemPlacementContext) == null) {
                    throw new ActionResultException1_12_2(InteractionResult.PASS);
                }
            }

            this.connection.send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            if (itemStack.isEmpty()) {
                throw new ActionResultException1_12_2(InteractionResult.PASS);
            }
            final UseOnContext itemUsageContext = new UseOnContext(player, hand, checkHitResult);
            InteractionResult actionResult;
            if (this.localPlayerMode.isCreative()) {
                final int count = itemStack.getCount();
                actionResult = itemStack.useOn(itemUsageContext);
                itemStack.setCount(count);
            } else {
                actionResult = itemStack.useOn(itemUsageContext);
            }
            if (!actionResult.consumesAction()) {
                actionResult = InteractionResult.PASS; // In <= 1.12.2 FAIL is the same as PASS
            }
            throw new ActionResultException1_12_2(actionResult);
        }
    }

    @Inject(method = "lambda$useItem$5", at = @At("HEAD"))
    private void trackLastUsedItem(InteractionHand hand, Player playerEntity, MutableObject<InteractionResult> mutableObject, int sequence, CallbackInfoReturnable<Packet<?>> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            ViaFabricPlusHandItemProvider.lastUsedItem = playerEntity.getItemInHand(hand).copy();
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void cancelOffHandItemInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void cancelOffHandBlockPlace(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    /**
     * @author RK_01
     * @reason Block place fix
     */
    @Overwrite
    private Packet<?> lambda$useItemOn$4(MutableObject<InteractionResult> mutableObject, LocalPlayer clientPlayerEntity, InteractionHand hand, BlockHitResult blockHitResult, int sequence) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            ViaFabricPlusHandItemProvider.lastUsedItem = clientPlayerEntity.getItemInHand(hand).copy();
        }
        try {
            mutableObject.setValue(this.performUseItemOn(clientPlayerEntity, hand, blockHitResult));
            return new ServerboundUseItemOnPacket(hand, blockHitResult, sequence);
        } catch (ActionResultException1_12_2 e) {
            mutableObject.setValue(e.getActionResult());
            throw e;
        }
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"))
    private void catchPacketCancelException(MultiPlayerGameMode instance, ClientLevel world, PredictiveAction packetCreator) {
        try {
            this.startPrediction(world, packetCreator);
        } catch (ActionResultException1_12_2 ignored) {
        }
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void removeClickActions(int syncId, int slotId, int button, ClickType actionType, Player player, CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_5tob1_5_2) && !actionType.equals(ClickType.PICKUP)) {
            ci.cancel();
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7) && !actionType.equals(ClickType.PICKUP) && !actionType.equals(ClickType.QUICK_MOVE) && !actionType.equals(ClickType.SWAP) && !actionType.equals(ClickType.CLONE)) {
            ci.cancel();
        }
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2) && actionType == ClickType.SWAP && button == 40) { // Pressing 'F' in inventory
            ci.cancel();
        }
    }

    @Inject(method = "hasExperience", at = @At("HEAD"), cancellable = true)
    private void removeExperienceBar(CallbackInfoReturnable<Boolean> cir) {
        if (VisualSettings.global().removeNewerHudElements.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean viaFabricPlus$extinguishFire(BlockPos blockPos, final Direction direction) {
        blockPos = blockPos.relative(direction);
        if (this.minecraft.level.getBlockState(blockPos).getBlock() == Blocks.FIRE) {
            this.minecraft.level.levelEvent(this.minecraft.player, 1009, blockPos, 0);
            this.minecraft.level.removeBlock(blockPos, false);
            return true;
        }
        return false;
    }

    @Unique
    private boolean viaFabricPlus$shouldBeEmpty(final ClickType type, final int slot) {
        // quick craft always uses empty stack for verification
        if (type == ClickType.QUICK_CRAFT) return true;

        // Special case: throw always uses empty stack for verification
        if (type == ClickType.THROW) return true;

        // quick move always uses empty stack for verification since 1.12
        if (type == ClickType.QUICK_MOVE && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_11_1)) return true;

        // pickup with slot -999 (outside window) to throw items always uses empty stack for verification
        return type == ClickType.PICKUP && slot == -999;
    }

    @Override
    public ClientPlayerInteractionManager1_18_2 viaFabricPlus$get1_18_2InteractionManager() {
        return this.viaFabricPlus$1_18_2InteractionManager;
    }

}
