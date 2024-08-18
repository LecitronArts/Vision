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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DiggerItem.class)
public abstract class MixinMiningToolItem extends TieredItem {

    @Shadow
    @Final
    private float attackDamageBaseline;

    @Shadow
    @Final
    private Multimap<Attribute, AttributeModifier> defaultModifiers;

    @Unique
    private float viaFabricPlus$attackDamage_r1_8;

    @Unique
    private Multimap<Attribute, AttributeModifier> viaFabricPlus$AttributeModifiers_r1_8;

    public MixinMiningToolItem(Tier material, Properties settings) {
        super(material, settings);
    }

/*    @Inject(method = "<init>", at = @At("RETURN"))
    private void init1_8Fields(float attackDamage, float attackSpeed, Tier material, TagKey<Block> effectiveBlocks, Properties settings, CallbackInfo ci) {
        final float materialAttackDamage = material.getAttackDamageBonus();
        if ((Item) this instanceof PickaxeItem) {
            this.viaFabricPlus$attackDamage_r1_8 = 2 + materialAttackDamage;
        } else if ((Item) this instanceof ShovelItem) {
            this.viaFabricPlus$attackDamage_r1_8 = 1 + materialAttackDamage;
        } else if ((Item) this instanceof AxeItem) {
            this.viaFabricPlus$attackDamage_r1_8 = 3 + materialAttackDamage;
        } else { // HoeItem didn't use MiningToolItem abstraction in 1.8
            this.viaFabricPlus$AttributeModifiers_r1_8 = ImmutableMultimap.of();
            return;
        }

        final ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", this.viaFabricPlus$attackDamage_r1_8, AttributeModifier.Operation.ADDITION));
        this.viaFabricPlus$AttributeModifiers_r1_8 = builder.build();
    }*/

/*    @Redirect(method = "getAttackDamage", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/DiggerItem;attackDamageBaseline:F"))
    private float changeAttackDamage(DiggerItem instance) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return this.viaFabricPlus$attackDamage_r1_8;
        } else {
            return this.attackDamageBaseline;
        }
    }*/

/*    @Redirect(method = "getDefaultAttributeModifiers", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/DiggerItem;defaultModifiers:Lcom/google/common/collect/Multimap;"))
    private Multimap<Attribute, AttributeModifier> changeAttributeModifiers(DiggerItem instance) {
        if (DebugSettings.global().replaceAttributeModifiers.isEnabled()) {
            return this.viaFabricPlus$AttributeModifiers_r1_8;
        } else {
            return this.defaultModifiers;
        }
    }*/

}
