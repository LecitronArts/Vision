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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.tracker.WolfHealthTracker;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class MixinWolfEntity extends TamableAnimal implements NeutralMob {

    @Shadow
    public abstract DyeColor getCollarColor();

    @Shadow
    public abstract void setCollarColor(DyeColor color);

    protected MixinWolfEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void fixWolfInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            final ItemStack itemStack = player.getItemInHand(hand);
            final Item item = itemStack.getItem();
            if (this.isTame()) {
                if (item.isEdible()) {
                    if (item.getFoodProperties().isMeat() && this.viaFabricPlus$getWolfHealth() < 20.0F) {
                        if (!player.getAbilities().instabuild) itemStack.shrink(1);
                        this.heal((float) item.getFoodProperties().getNutrition());
                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                } else if (item instanceof DyeItem dyeItem) {
                    final DyeColor dyeColor = dyeItem.getDyeColor();
                    if (dyeColor != this.getCollarColor()) {
                        this.setCollarColor(dyeColor);
                        if (!player.getAbilities().instabuild) itemStack.shrink(1);
                        cir.setReturnValue(InteractionResult.SUCCESS);
                        return;
                    }
                }
            } else if (item == Items.BONE && !this.isAngry()) {
                if (!player.getAbilities().instabuild) itemStack.shrink(1);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            cir.setReturnValue(super.mobInteract(player, hand));
        }
    }
    //skip it, because it doesn't work
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;getHealth()F"))
    private float fixWolfHealth(Wolf instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            return this.viaFabricPlus$getWolfHealth();
        } else {
            return instance.getHealth();
        }
    }


    @Unique
    private float viaFabricPlus$getWolfHealth() {
        return WolfHealthTracker.get(ProtocolTranslator.getPlayNetworkUserConnection()).getWolfHealth(this.getId(), this.getHealth());
    }

}
