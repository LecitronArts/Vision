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

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import java.util.*;
import java.util.function.Supplier;

/**
 * A helper class for creating recipes.
 */
public final class RecipeInfo {

    private final Supplier<Recipe<?>> creator;

    private RecipeInfo(Supplier<Recipe<?>> creator) {
        this.creator = creator;
    }

    /**
     * Creates a new recipe info with the given creator.
     *
     * @param creator The creator
     * @return The recipe info
     */
    public static RecipeInfo of(Supplier<Recipe<?>> creator) {
        return new RecipeInfo(creator);
    }

    /**
     * Creates a new shaped recipe info with the given creator.
     *
     * @param output The output
     * @param args   The arguments
     * @return The recipe info containing a shaped recipe
     */
    public static RecipeInfo shaped(ItemLike output, Object... args) {
        return shaped("", output, args);
    }

    /**
     * Creates a new shaped recipe info with the given creator.
     *
     * @param count  The count
     * @param output The output
     * @param args   The arguments
     * @return The recipe info containing a shaped recipe
     */
    public static RecipeInfo shaped(int count, ItemLike output, Object... args) {
        return shaped("", count, output, args);
    }

    /**
     * Creates a new shaped recipe info with the given creator.
     *
     * @param group  The group
     * @param output The output
     * @param args   The arguments
     * @return The recipe info containing a shaped recipe
     */
    public static RecipeInfo shaped(String group, ItemStack output, Object... args) {
        final List<String> shape = new ArrayList<>();

        int i;
        int width = 0;
        for (i = 0; i < args.length && args[i] instanceof String str; i++) {
            if (i == 0) {
                width = str.length();
            } else if (str.length() != width) {
                throw new IllegalArgumentException("Rows do not have consistent width");
            }
            shape.add(str);
        }
        Map<Character, Ingredient> legend = new HashMap<>();
        while (i < args.length && args[i] instanceof Character key) {
            i++;
            List<ItemLike> items = new ArrayList<>();
            for (; i < args.length && args[i] instanceof ItemLike; i++) {
                items.add((ItemLike) args[i]);
            }
            legend.put(key, Ingredient.of(items.toArray(new ItemLike[0])));
        }
        if (i != args.length) {
            throw new IllegalArgumentException("Unexpected argument at index " + i + ": " + args[i]);
        }

        final int height = shape.size();
        final NonNullList<Ingredient> ingredients = NonNullList.create();
        for (String row : shape) {
            for (int x = 0; x < width; x++) {
                final char key = row.charAt(x);
                Ingredient ingredient = legend.get(key);
                if (ingredient == null) {
                    if (key == ' ') {
                        ingredient = Ingredient.EMPTY;
                    } else {
                        throw new IllegalArgumentException("Unknown character in shape: " + key);
                    }
                }
                ingredients.add(ingredient);
            }
        }

        final int width_f = width;
        return new RecipeInfo(() -> new ShapedRecipe(group, CraftingBookCategory.MISC, new ShapedRecipePattern(width_f, height, ingredients, Optional.empty()), output, false));
    }

    /**
     * Creates a new shaped recipe info with the given creator.
     *
     * @param group  The group
     * @param output The output
     * @param args   The arguments
     * @return The recipe info containing a shaped recipe
     */
    public static RecipeInfo shaped(String group, ItemLike output, Object... args) {
        return shaped(group, new ItemStack(output), args);
    }

    /**
     * Creates a new shaped recipe info with the given creator.
     *
     * @param group  The group
     * @param count  The count
     * @param output The output
     * @param args   The arguments
     * @return The recipe info containing a shaped recipe
     */
    public static RecipeInfo shaped(String group, int count, ItemLike output, Object... args) {
        return shaped(group, new ItemStack(output, count), args);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param group  The group
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(String group, ItemStack output, ItemLike... inputs) {
        final ItemLike[][] newInputs = new ItemLike[inputs.length][1];
        for (int i = 0; i < inputs.length; i++) {
            newInputs[i] = new ItemLike[]{inputs[i]};
        }
        return shapeless(group, output, newInputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param group  The group
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(String group, ItemLike output, ItemLike... inputs) {
        return shapeless(group, new ItemStack(output), inputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param group  The group
     * @param count  The count
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(String group, int count, ItemLike output, ItemLike... inputs) {
        return shapeless(group, new ItemStack(output, count), inputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param group  The group
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(String group, ItemStack output, ItemLike[]... inputs) {
        final NonNullList<Ingredient> ingredients = NonNullList.create();
        for (ItemLike[] input : inputs) {
            ingredients.add(Ingredient.of(input));
        }
        return new RecipeInfo(() -> new ShapelessRecipe(group, CraftingBookCategory.MISC, output, ingredients));
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param group  The group
     * @param count  The count
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(String group, int count, ItemLike output, ItemLike[]... inputs) {
        return shapeless(group, new ItemStack(output, count), inputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(ItemLike output, ItemLike... inputs) {
        return shapeless("", output, inputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param count  The count
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(int count, ItemLike output, ItemLike... inputs) {
        return shapeless("", count, output, inputs);
    }

    /**
     * Creates a new shapeless recipe info with the given creator.
     *
     * @param count  The count
     * @param output The output
     * @param inputs The inputs
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo shapeless(int count, ItemLike output, ItemLike[]... inputs) {
        return shapeless("", count, output, inputs);
    }

    /**
     * Creates a new smelting recipe info with the given creator.
     *
     * @param output     The output
     * @param input      The input
     * @param experience The experience
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo smelting(ItemLike output, ItemLike input, float experience) {
        return smelting(output, input, experience, 200);
    }

    /**
     * Creates a new smelting recipe info with the given creator.
     *
     * @param output     The output
     * @param input      The input
     * @param experience The experience
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo smelting(ItemLike output, Ingredient input, float experience) {
        return smelting(output, input, experience, 200);
    }

    /**
     * Creates a new smelting recipe info with the given creator.
     *
     * @param output     The output
     * @param input      The input
     * @param experience The experience
     * @param cookTime   The cook time
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo smelting(ItemLike output, ItemLike input, float experience, int cookTime) {
        return smelting(output, Ingredient.of(input), experience, cookTime);
    }

    /**
     * Creates a new smelting recipe info with the given creator.
     *
     * @param output     The output
     * @param input      The input
     * @param experience The experience
     * @param cookTime   The cook time
     * @return The recipe info containing a shapeless recipe
     */
    public static RecipeInfo smelting(ItemLike output, Ingredient input, float experience, int cookTime) {
        return new RecipeInfo(() -> new SmeltingRecipe("", CookingBookCategory.MISC, input, new ItemStack(output), experience, cookTime));
    }

    /**
     * Creates a new recipe info with the given creator.
     *
     * @param id The id
     * @return The recipe info
     */
    public RecipeHolder<?> create(ResourceLocation id) {
        return new RecipeHolder<Recipe<?>>(id, this.creator.get());
    }

}
