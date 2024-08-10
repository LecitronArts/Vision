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

import com.mojang.blaze3d.systems.RenderSystem;
import de.florianmichael.classic4j.ClassiCubeHandler;
import de.florianmichael.classic4j.api.LoginProcessHandler;
import de.florianmichael.classic4j.model.classicube.server.CCServerInfo;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.access.IServerInfo;
import de.florianmichael.viafabricplus.protocoltranslator.impl.provider.vialegacy.ViaFabricPlusClassicMPPassProvider;
import de.florianmichael.viafabricplus.screen.VFPListEntry;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.settings.impl.AuthenticationSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.util.ArrayList;
import java.util.List;

public class ClassiCubeServerListScreen extends VFPScreen {

    public static final ClassiCubeServerListScreen INSTANCE = new ClassiCubeServerListScreen();

    public static final List<CCServerInfo> SERVER_LIST = new ArrayList<>();
    private static final String CLASSICUBE_SERVER_LIST_URL = "https://www.classicube.net/server/list/";

    public static void open(final Screen prevScreen, final LoginProcessHandler loginProcessHandler) {
        final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount();

        ClassiCubeHandler.requestServerList(account, serverList -> {
            ClassiCubeServerListScreen.SERVER_LIST.addAll(serverList.servers());
            RenderSystem.recordRenderCall(() -> ClassiCubeServerListScreen.INSTANCE.open(prevScreen));
        }, loginProcessHandler::handleException);
    }

    public ClassiCubeServerListScreen() {
        super("ClassiCube", true);
    }

    @Override
    protected void init() {
        final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount();
        if (account != null) {
            this.setupUrlSubtitle(CLASSICUBE_SERVER_LIST_URL);
        }

        this.addRenderableWidget(new SlotList(this.minecraft, width, height, 3 + 3 /* start offset */ + (font.lineHeight + 2) * 3 /* title is 2 */, -5, (font.lineHeight + 4) * 3));

        this.addRenderableWidget(Button.builder(Component.translatable("base.viafabricplus.logout"), button -> {
            onClose();
            ViaFabricPlus.global().getSaveManager().getAccountsSave().setClassicubeAccount(null);
            SERVER_LIST.clear();
        }).pos(width - 98 - 5, 5).size(98, 20).build());

        super.init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.renderTitle(context);

        final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount();
        if (account != null) {
            context.drawString(font, Component.nullToEmpty("ClassiCube Profile:"), 32, 6, -1);
            context.drawString(font, Component.nullToEmpty(account.username()), 32, 16, -1);
        }
    }

    public static class SlotList extends ObjectSelectionList<VFPListEntry> {

        public SlotList(Minecraft minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraftClient, width, height - top - bottom, top, entryHeight);

            SERVER_LIST.forEach(serverInfo -> this.addEntry(new ServerSlot(serverInfo)));
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
        private final CCServerInfo classiCubeServerInfo;

        public ServerSlot(CCServerInfo classiCubeServerInfo) {
            this.classiCubeServerInfo = classiCubeServerInfo;
        }

        @Override
        public Component getNarration() {
            return Component.literal(classiCubeServerInfo.name());
        }

        @Override
        public void mappedMouseClicked(double mouseX, double mouseY, int button) {
            final ServerAddress serverAddress = ServerAddress.parseString(classiCubeServerInfo.ip() + ":" + classiCubeServerInfo.port());
            final ServerData entry = new ServerData(classiCubeServerInfo.name(), serverAddress.getHost(), ServerData.Type.OTHER);
            ViaFabricPlusClassicMPPassProvider.classicMpPassForNextJoin = classiCubeServerInfo.mpPass();

            if (AuthenticationSettings.global().automaticallySelectCPEInClassiCubeServerList.getValue()) {
                ((IServerInfo) entry).viaFabricPlus$forceVersion(LegacyProtocolVersion.c0_30cpe);
            }

            ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), serverAddress, entry, false);
            super.mappedMouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void mappedRender(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final Font textRenderer = Minecraft.getInstance().font;
            context.drawCenteredString(textRenderer, classiCubeServerInfo.name(), entryWidth / 2, entryHeight / 2 - textRenderer.lineHeight / 2, -1);

            context.drawString(textRenderer, classiCubeServerInfo.software().replace('&', ChatFormatting.PREFIX_CODE), 1, 1, -1);
            final String playerText = classiCubeServerInfo.players() + "/" + classiCubeServerInfo.maxPlayers();
            context.drawString(textRenderer, playerText, entryWidth - textRenderer.width(playerText) - 4 /* magic value from line 132 */ - 1, 1, -1);
        }
    }

}
