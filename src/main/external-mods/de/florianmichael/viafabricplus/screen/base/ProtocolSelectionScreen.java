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

package de.florianmichael.viafabricplus.screen.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.screen.settings.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.raphimc.vialoader.util.ProtocolVersionList;

import java.awt.*;

public class ProtocolSelectionScreen extends VFPScreen {

    public static final ProtocolSelectionScreen INSTANCE = new ProtocolSelectionScreen();

    protected ProtocolSelectionScreen() {
        super("ViaFabricPlus", true);
    }

    @Override
    protected void init() {
        // List and Settings
        this.setupDefaultSubtitle();
        this.addRenderableWidget(new SlotList(this.minecraft, width, height, 3 + 3 /* start offset */ + (font.lineHeight + 2) * 3 /* title is 2 */, 30, font.lineHeight + 4));
        this.addRenderableWidget(Button.builder(Component.translatable("base.viafabricplus.settings"), button -> SettingsScreen.INSTANCE.open(this)).pos(width - 98 - 5, 5).size(98, 20).build());

        this.addRenderableWidget(Button.builder(ServerListScreen.INSTANCE.getTitle(), button -> ServerListScreen.INSTANCE.open(this))
                .pos(5, height - 25).size(98, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("report.viafabricplus.button"), button -> ReportIssuesScreen.INSTANCE.open(this))
                .pos(width - 98 - 5, height - 25).size(98, 20).build());

        super.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        this.renderTitle(context);
    }

    public static class SlotList extends ObjectSelectionList<ProtocolSlot> {

        public SlotList(Minecraft minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraftClient, width, height - top - bottom, top, entryHeight);

            ProtocolVersionList.getProtocolsNewToOld().stream().map(ProtocolSlot::new).forEach(this::addEntry);
        }
    }

    public static class ProtocolSlot extends ObjectSelectionList.Entry<ProtocolSlot> {

        private final ProtocolVersion protocolVersion;

        public ProtocolSlot(final ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.protocolVersion.getName());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ProtocolTranslator.setTargetVersion(this.protocolVersion);
            playClickSound();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final boolean isSelected = ProtocolTranslator.getTargetVersion().equals(protocolVersion);

            final PoseStack matrices = context.pose();

            matrices.pushPose();
            matrices.translate(x, y - 1, 0);

            final Font textRenderer = Minecraft.getInstance().font;
            context.drawCenteredString(textRenderer, this.protocolVersion.getName(), entryWidth / 2, entryHeight / 2 - textRenderer.lineHeight / 2, isSelected ? Color.GREEN.getRGB() : Color.RED.getRGB());
            matrices.popPose();
        }
    }

}
