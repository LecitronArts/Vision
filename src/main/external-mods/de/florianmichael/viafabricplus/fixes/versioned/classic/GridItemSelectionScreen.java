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

package de.florianmichael.viafabricplus.fixes.versioned.classic;

import de.florianmichael.viafabricplus.fixes.data.ItemRegistryDiff;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("DataFlowIssue")
public class GridItemSelectionScreen extends VFPScreen {

    public static final GridItemSelectionScreen INSTANCE = new GridItemSelectionScreen();

    private static final int MAX_ROW_DIVIDER = 9;
    private static final int ITEM_XY_BOX_DIMENSION_CLASSIC = 25;
    private static final int SIDE_OFFSET = 15;
    private static final int ITEM_XY_BOX_DIMENSION_MODERN = 16;

    public Item[][] itemGrid = null;
    public ItemStack selectedItem = null;

    public GridItemSelectionScreen() {
        super("Classic item selection", false);
    }

    @Override
    protected void init() {
        if (itemGrid != null) {
            return;
        }
        final List<Item> allowedItems = new ArrayList<>();
        // Calculate all visible items
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !item.requiredFeatures().contains(FeatureFlags.VANILLA)) continue;
            if (ItemRegistryDiff.keepItem(item)) {
                allowedItems.add(item);
            }
        }

        itemGrid = new Item[Mth.ceil(allowedItems.size() / (double) MAX_ROW_DIVIDER)][MAX_ROW_DIVIDER];
        int x = 0;
        int y = 0;
        for (Item allowedItem : allowedItems) {
            itemGrid[y][x] = allowedItem;
            x++;
            if (x == MAX_ROW_DIVIDER) {
                x = 0;
                y++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedItem != null) {
            this.minecraft.gameMode.handleCreativeModeItemAdd(selectedItem, minecraft.player.getInventory().selected + 36); // Beta Inventory Tracker

            this.minecraft.player.getInventory().items.set(minecraft.player.getInventory().selected, selectedItem);
            this.minecraft.player.inventoryMenu.broadcastChanges();

            playClickSound();
            this.onClose();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        final int halfWidth = this.width / 2;
        final int halfHeight = this.height / 2;

        final int boxWidth = ITEM_XY_BOX_DIMENSION_CLASSIC * MAX_ROW_DIVIDER + SIDE_OFFSET * 2;
        final int boxHeight = ITEM_XY_BOX_DIMENSION_CLASSIC * itemGrid.length + SIDE_OFFSET * 2 + SIDE_OFFSET;

        final int renderX = halfWidth - boxWidth / 2;
        final int renderY = halfHeight - boxHeight / 2;

        context.fill(renderX, renderY, renderX + boxWidth, renderY + boxHeight, Integer.MIN_VALUE);
        context.drawCenteredString(font, "Select block", renderX + boxWidth / 2, renderY + SIDE_OFFSET, -1);
        selectedItem = null;

        int y = SIDE_OFFSET + SIDE_OFFSET;
        for (Item[] items : itemGrid) {
            int x = SIDE_OFFSET;
            for (Item item : items) {
                if (item == null) continue;

                if (mouseX > renderX + x && mouseY > renderY + y && mouseX < renderX + x + ITEM_XY_BOX_DIMENSION_CLASSIC && mouseY < renderY + y + ITEM_XY_BOX_DIMENSION_CLASSIC) {
                    context.fill(renderX + x, renderY + y, renderX + x + ITEM_XY_BOX_DIMENSION_CLASSIC, renderY + y + ITEM_XY_BOX_DIMENSION_CLASSIC, Integer.MAX_VALUE);
                    selectedItem = item.getDefaultInstance();
                }
                context.renderItem(item.getDefaultInstance(), renderX + x + ITEM_XY_BOX_DIMENSION_MODERN / 4, renderY + y + ITEM_XY_BOX_DIMENSION_MODERN / 4);
                x += ITEM_XY_BOX_DIMENSION_CLASSIC;
            }
            y += ITEM_XY_BOX_DIMENSION_CLASSIC;
        }
    }

}
