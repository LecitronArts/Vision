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

package de.florianmichael.viafabricplus.screen.classic4j;

import de.florianmichael.classic4j.model.betacraft.BCServerInfoSpec;
import de.florianmichael.classic4j.model.betacraft.BCServerList;
import de.florianmichael.classic4j.model.betacraft.BCVersionCategory;
import de.florianmichael.viafabricplus.screen.VFPListEntry;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.screen.settings.TitleRenderer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public class BetaCraftScreen extends VFPScreen {

    public static final BetaCraftScreen INSTANCE = new BetaCraftScreen();

    public static BCServerList SERVER_LIST;
    private static final String BETA_CRAFT_SERVER_LIST_URL = "https://betacraft.uk/serverlist/";

    protected BetaCraftScreen() {
        super("BetaCraft", true);
    }

    @Override
    protected void init() {
        this.setupSubtitle(Component.nullToEmpty(BETA_CRAFT_SERVER_LIST_URL), ConfirmLinkScreen.confirmLink(this, BETA_CRAFT_SERVER_LIST_URL));
        this.addRenderableWidget(new SlotList(this.minecraft, width, height, 3 + 3 /* start offset */ + (font.lineHeight + 2) * 3 /* title is 2 */, -5, (font.lineHeight + 2) * 3));

        this.addRenderableWidget(Button.builder(Component.translatable("base.viafabricplus.reset"), button -> {
            SERVER_LIST = null;
            minecraft.setScreen(prevScreen);
        }).pos(width - 98 - 5, 5).size(98, 20).build());

        super.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        this.renderTitle(context);
    }

    public static class SlotList extends ObjectSelectionList<VFPListEntry> {

        public SlotList(Minecraft minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraftClient, width, height - top - bottom, top, entryHeight);
            if (SERVER_LIST == null) return;

            for (BCVersionCategory value : BCVersionCategory.values()) {
                final List<BCServerInfoSpec> servers = SERVER_LIST.serversOfVersionCategory(value);
                if (servers.isEmpty()) continue;
                addEntry(new TitleRenderer(Component.literal(value.name())));
                for (BCServerInfoSpec server : servers) {
                    addEntry(new ServerSlot(server));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 140;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 5;
        }
    }

    public static class ServerSlot extends VFPListEntry {
        private final BCServerInfoSpec server;

        public ServerSlot(BCServerInfoSpec server) {
            this.server = server;
        }

        @Override
        public Component getNarration() {
            return Component.literal(server.name());
        }

        @Override
        public void mappedMouseClicked(double mouseX, double mouseY, int button) {
            final ServerAddress serverAddress = ServerAddress.parseString(server.socketAddress());
            final ServerData entry = new ServerData(server.name(), serverAddress.getHost(), ServerData.Type.OTHER);

            ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), serverAddress, entry, false);
            super.mappedMouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void mappedRender(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final Font textRenderer = Minecraft.getInstance().font;
            context.drawCenteredString(textRenderer, server.name() + ChatFormatting.DARK_GRAY + " [" + server.connectVersion() + "]", entryWidth / 2, entryHeight / 2 - textRenderer.lineHeight / 2, -1);

            if (server.onlineMode()) {
                context.drawString(textRenderer, Component.translatable("base.viafabricplus.online_mode").withStyle(ChatFormatting.GREEN), 1, 1, -1);
            }
            final String playerText = server.playerCount() + "/" + server.playerLimit();
            context.drawString(textRenderer, playerText, entryWidth - textRenderer.width(playerText) - 4 /* magic value from line 152 */ - 1, 1, -1);
        }
    }

}
