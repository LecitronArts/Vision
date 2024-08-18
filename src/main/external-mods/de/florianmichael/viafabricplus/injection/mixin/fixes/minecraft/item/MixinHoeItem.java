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
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(HoeItem.class)
public abstract class MixinHoeItem extends DiggerItem {

    @Unique
    private static final Set<Block> viaFabricPlus$effective_blocks_r1_16_5 = ImmutableSet.of(
            Blocks.NETHER_WART_BLOCK,
            Blocks.WARPED_WART_BLOCK,
            Blocks.HAY_BLOCK,
            Blocks.DRIED_KELP_BLOCK,
            Blocks.TARGET,
            Blocks.SHROOMLIGHT,
            Blocks.SPONGE,
            Blocks.WET_SPONGE,
            Blocks.JUNGLE_LEAVES,
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.BIRCH_LEAVES
    );

    protected MixinHoeItem(float attackDamage, float attackSpeed, Tier material, TagKey<Block> effectiveBlocks, Properties settings) {
        super(attackDamage, attackSpeed, material, effectiveBlocks, settings);
    }

/*
    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_16_4) && super.isCorrectToolForDrops(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return 1.0F;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            return viaFabricPlus$effective_blocks_r1_16_5.contains(state.getBlock()) ? this.speed : 1.0F;
        } else {
            return super.getDestroySpeed(stack, state);
        }
    }
*/

}
