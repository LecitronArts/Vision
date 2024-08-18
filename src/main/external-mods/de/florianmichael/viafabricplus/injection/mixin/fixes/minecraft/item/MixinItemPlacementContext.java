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
import de.florianmichael.viafabricplus.fixes.data.Material1_19_4;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
public abstract class MixinItemPlacementContext extends UseOnContext {

    public MixinItemPlacementContext(Player player, InteractionHand hand, BlockHitResult hit) {
        super(player, hand, hit);
    }

/*    @Inject(method = "getNearestLookingDirection", at = @At("HEAD"), cancellable = true)
    private void getPlayerLookDirection1_12_2(CallbackInfoReturnable<Direction> cir) {
        final BlockPlaceContext self = (BlockPlaceContext) (Object) this;
        final Player player = self.getPlayer();

        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            final BlockPos placementPos = self.getClickedPos();
            final double blockPosCenterFactor = ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10) ? 0.5 : 0;

            if (Math.abs(player.getX() - (placementPos.getX() + blockPosCenterFactor)) < 2 && Math.abs(player.getZ() - (placementPos.getZ() + blockPosCenterFactor)) < 2) {
                final double eyeY = player.getY() + player.getEyeHeight(player.getPose());

                if (eyeY - placementPos.getY() > 2) {
                    cir.setReturnValue(Direction.DOWN);
                    return;
                }

                if (placementPos.getY() - eyeY > 0) {
                    cir.setReturnValue(Direction.UP);
                    return;
                }
            }

            cir.setReturnValue(player.getDirection());
        }
    }*/

/*    @Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
    private void canPlace1_12_2(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(Material1_19_4.getMaterial(this.getLevel().getBlockState(this.getClickedPos())).equals(Material1_19_4.DECORATION) && Block.byItem(this.getItemInHand().getItem()).equals(Blocks.ANVIL));
        }
    }*/

}
