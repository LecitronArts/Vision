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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.raphimc.vialoader.util.ProtocolVersionList;

import java.util.function.Consumer;

public class PerServerVersionScreen extends VFPScreen {

    private final Consumer<ProtocolVersion> selectionConsumer;

    public PerServerVersionScreen(final Screen prevScreen, final Consumer<ProtocolVersion> selectionConsumer) {
        super("Force version", false);

        this.prevScreen = prevScreen;
        this.selectionConsumer = selectionConsumer;

        this.setupSubtitle(Component.translatable("base.viafabricplus.force_version_title"));
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(new SlotList(this.minecraft, width, height, 3 + 3 /* start offset */ + (font.lineHeight + 2) * 3 /* title is 2 */, -5, font.lineHeight + 4));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        this.renderTitle(context);
    }

    public class SlotList extends ObjectSelectionList<SharedSlot> {

        public SlotList(Minecraft minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraftClient, width, height - top - bottom, top, entryHeight);

            this.addEntry(new ResetSlot());
            ProtocolVersionList.getProtocolsNewToOld().stream().map(ProtocolSlot::new).forEach(this::addEntry);
        }
    }

    // Dummy class files used to have a shared superclass for ResetSlot and ProtocolSlot
    public abstract class SharedSlot extends ObjectSelectionList.Entry<SharedSlot> {

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            playClickSound();
            onClose();
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public class ResetSlot extends SharedSlot {

        @Override
        public Component getNarration() {
            return Component.translatable("base.viafabricplus.cancel_and_reset");
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectionConsumer.accept(null);

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final Font textRenderer = Minecraft.getInstance().font;
            context.drawCenteredString(textRenderer, ((MutableComponent) getNarration()).withStyle(ChatFormatting.GOLD), x + entryWidth / 2, y + entryHeight / 2 - textRenderer.lineHeight / 2, -1);
        }
    }

    public class ProtocolSlot extends SharedSlot {

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
            selectionConsumer.accept(protocolVersion);

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final Font textRenderer = Minecraft.getInstance().font;
            context.drawCenteredString(textRenderer, this.protocolVersion.getName(), x + entryWidth / 2, y - 1 + entryHeight / 2 - textRenderer.lineHeight / 2, -1);
        }
    }

}
