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

package de.florianmichael.viafabricplus.fixes.data.recipe;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;

public class AddBannerPatternRecipe extends CustomRecipe {

    public static final RecipeSerializer<AddBannerPatternRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(AddBannerPatternRecipe::new);

    public AddBannerPatternRecipe(CraftingBookCategory craftingRecipeCategory) {
        super(craftingRecipeCategory);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        boolean foundBanner = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof BannerItem) {
                if (foundBanner)
                    return false;
                if (BannerBlockEntity.getPatternCount(stack) >= 6)
                    return false;
                foundBanner = true;
            }
        }
        return foundBanner && getBannerPattern(inv) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryManager) {
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BannerItem) {
                result = stack.copy();
                result.setCount(1);
                break;
            }
        }

        BannerPattern_1_13_2 pattern = getBannerPattern(inv);
        if (pattern != null) {
            DyeColor color = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) ? DyeColor.BLACK : DyeColor.WHITE;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                Item item = inv.getItem(i).getItem();
                if (item instanceof DyeItem dyeItem) {
                    color = dyeItem.getDyeColor();
                }
            }

            CompoundTag tileEntityNbt = result.getOrCreateTagElement("BlockEntityTag");
            ListTag patterns;
            if (tileEntityNbt.contains("Patterns", 9)) {
                patterns = tileEntityNbt.getList("Patterns", 10);
            } else {
                patterns = new ListTag();
                tileEntityNbt.put("Patterns", patterns);
            }
            CompoundTag patternNbt = new CompoundTag();
            patternNbt.putString("Pattern", pattern.getId());
            patternNbt.putInt("Color", color.getId());
            patterns.add(patternNbt);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<AddBannerPatternRecipe> getSerializer() {
        return SERIALIZER;
    }

    private static BannerPattern_1_13_2 getBannerPattern(CraftingContainer inv) {
        for (BannerPattern_1_13_2 pattern : BannerPattern_1_13_2.values()) {
            if (!pattern.isCraftable())
                continue;

            boolean matches = true;
            if (pattern.hasBaseStack()) {
                boolean foundBaseItem = false;
                boolean foundDye = false;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && !(stack.getItem() instanceof BannerItem)) {
                        if (stack.getItem() instanceof DyeItem) {
                            if (foundDye) {
                                matches = false;
                                break;
                            }
                            foundDye = true;
                        } else {
                            if (foundBaseItem || !ItemStack.isSameItem(stack, pattern.getBaseStack())) {
                                matches = false;
                                break;
                            }
                            foundBaseItem = true;
                        }
                    }
                }
                if (!foundBaseItem || (!foundDye && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10))) matches = false;
            } else if (inv.getContainerSize() == pattern.getRecipePattern().length * pattern.getRecipePattern()[0].length()) {
                DyeColor patternColor = null;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    int row = i / 3;
                    int col = i % 3;
                    ItemStack stack = inv.getItem(i);
                    Item item = stack.getItem();
                    if (!stack.isEmpty() && !(item instanceof BannerItem)) {
                        if (!(item instanceof DyeItem)) {
                            matches = false;
                            break;
                        }

                        DyeColor color = ((DyeItem) item).getDyeColor();
                        if (patternColor != null && color != patternColor) {
                            matches = false;
                            break;
                        }

                        if (pattern.getRecipePattern()[row].charAt(col) == ' ') {
                            matches = false;
                            break;
                        }

                        patternColor = color;
                    } else if (pattern.getRecipePattern()[row].charAt(col) != ' ') {
                        matches = false;
                        break;
                    }
                }
            } else {
                matches = false;
            }

            if (matches)
                return pattern;
        }

        return null;
    }

}
