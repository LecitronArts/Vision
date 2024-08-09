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

import com.google.common.collect.ImmutableSet;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.data.Material1_19_4;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(AxeItem.class)
public abstract class MixinAxeItem extends DiggerItem {

    @Unique
    private static final Set<Block> viaFabricPlus$effective_blocks_b1_8_1 = ImmutableSet.of(Blocks.OAK_PLANKS, Blocks.BOOKSHELF, Blocks.OAK_LOG, Blocks.CHEST);

    @Unique
    private static final Set<Block> viaFabricPlus$effective_blocks_r1_16_5 = ImmutableSet.of(Blocks.LADDER, Blocks.SCAFFOLDING, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON);

    @Unique
    private static final Set<Material1_19_4> viaFabricPlus$effective_materials_r1_16_5 = ImmutableSet.of(Material1_19_4.WOOD, Material1_19_4.NETHER_WOOD, Material1_19_4.PLANT, Material1_19_4.REPLACEABLE_PLANT, Material1_19_4.BAMBOO, Material1_19_4.GOURD);

    public MixinAxeItem(float attackDamage, float attackSpeed, Tier material, TagKey<Block> effectiveBlocks, Properties settings) {
        super(attackDamage, attackSpeed, material, effectiveBlocks, settings);
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            return false;
        } else {
            return super.isCorrectToolForDrops(state);
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void disableUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            return viaFabricPlus$effective_blocks_b1_8_1.contains(state.getBlock()) ? this.speed : 1.0F;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            return viaFabricPlus$effective_materials_r1_16_5.contains(Material1_19_4.getMaterial(state)) ? this.speed : viaFabricPlus$effective_blocks_r1_16_5.contains(state.getBlock()) ? this.speed : 1.0F;
        } else {
            return super.getDestroySpeed(stack, state);
        }
    }

}
