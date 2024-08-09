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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public abstract class MixinSwordItem extends TieredItem {

    @Shadow
    @Final
    private float attackDamage;

    @Shadow
    @Final
    private Multimap<Attribute, AttributeModifier> defaultModifiers;

    @Unique
    private float viaFabricPlus$attackDamage_r1_8;

    @Unique
    private Multimap<Attribute, AttributeModifier> viaFabricPlus$AttributeModifiers_r1_8;

    public MixinSwordItem(Tier material, Properties settings) {
        super(material, settings);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init1_8Fields(Tier toolMaterial, int attackDamage, float attackSpeed, Properties settings, CallbackInfo ci) {
        this.viaFabricPlus$attackDamage_r1_8 = 4 + toolMaterial.getAttackDamageBonus();
        final ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", this.viaFabricPlus$attackDamage_r1_8, AttributeModifier.Operation.ADDITION));
        this.viaFabricPlus$AttributeModifiers_r1_8 = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
            ItemStack itemStack = user.getItemInHand(hand);
            user.startUsingItem(hand);
            return InteractionResultHolder.consume(itemStack);
        } else {
            return super.use(world, user, hand);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
            return UseAnim.BLOCK;
        } else {
            return super.getUseAnimation(stack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
            return 72000;
        } else {
            return super.getUseDuration(stack);
        }
    }

    @Redirect(method = "getDamage", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/SwordItem;attackDamage:F"))
    private float changeAttackDamage(SwordItem instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return this.viaFabricPlus$attackDamage_r1_8;
        } else {
            return this.attackDamage;
        }
    }

    @Redirect(method = "getDefaultAttributeModifiers", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/SwordItem;defaultModifiers:Lcom/google/common/collect/Multimap;"))
    private Multimap<Attribute, AttributeModifier> changeAttributeModifiers(SwordItem instance) {
        if (DebugSettings.global().replaceAttributeModifiers.isEnabled()) {
            return this.viaFabricPlus$AttributeModifiers_r1_8;
        } else {
            return this.defaultModifiers;
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void changeMiningSpeed(ItemStack stack, BlockState state, CallbackInfoReturnable<Float> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            cir.setReturnValue(state.is(Blocks.COBWEB) ? 15.0F : 1.5F);
        }
    }

}
