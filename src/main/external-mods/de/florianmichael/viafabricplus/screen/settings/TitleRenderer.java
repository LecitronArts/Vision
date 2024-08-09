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

package de.florianmichael.viafabricplus.screen.settings;

import com.mojang.blaze3d.vertex.PoseStack;
import de.florianmichael.viafabricplus.screen.VFPListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TitleRenderer extends VFPListEntry {

    private final Component name;

    public TitleRenderer(Component name) {
        this.name = name;
    }

    @Override
    public Component getNarration() {
        return this.name;
    }

    @Override
    public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        final PoseStack matrices = context.pose();

        matrices.pushPose();
        matrices.translate(x, y, 0);
        mappedRender(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        matrices.popPose();
    }

    @Override
    public void mappedRender(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        final Font textRenderer = Minecraft.getInstance().font;

        context.drawString(textRenderer, this.name.copy().withStyle(ChatFormatting.BOLD), 3, entryHeight / 2 - textRenderer.lineHeight / 2, -1);
    }

}
