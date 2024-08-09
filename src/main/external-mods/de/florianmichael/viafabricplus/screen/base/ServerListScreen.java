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

import de.florianmichael.classic4j.BetaCraftHandler;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.screen.classic4j.BetaCraftScreen;
import de.florianmichael.viafabricplus.screen.classic4j.ClassiCubeLoginScreen;
import de.florianmichael.viafabricplus.screen.classic4j.ClassiCubeServerListScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class ServerListScreen extends VFPScreen {

    public static final ServerListScreen INSTANCE = new ServerListScreen();

    private Button betaCraftButton;

    public ServerListScreen() {
        super("Server lists", true);
    }

    @Override
    protected void init() {
        super.init();
        this.setupDefaultSubtitle();

        // ClassiCube
        final boolean loggedIn = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount() != null;

        Button.Builder classiCubeBuilder = Button.builder(ClassiCubeServerListScreen.INSTANCE.getTitle(), button -> {
            if (!loggedIn) {
                ClassiCubeLoginScreen.INSTANCE.open(this);
                return;
            }
            ClassiCubeServerListScreen.INSTANCE.open(this);
        }).pos(this.width / 2 - 50, this.height / 2 - 25).size(98, 20);
        if (!loggedIn) {
            classiCubeBuilder = classiCubeBuilder.tooltip(Tooltip.create(Component.translatable("classicube.viafabricplus.warning")));
        }
        this.addRenderableWidget(classiCubeBuilder.build());

        Button.Builder betaCraftBuilder = Button.builder(BetaCraftScreen.INSTANCE.getTitle(), button -> {
            if (BetaCraftScreen.SERVER_LIST == null) {
                betaCraftButton = button;

                BetaCraftHandler.requestV1ServerList(serverList -> {
                    BetaCraftScreen.SERVER_LIST = serverList;

                    BetaCraftScreen.INSTANCE.open(this);
                }, throwable -> showErrorScreen(BetaCraftScreen.INSTANCE.getTitle().getString(), throwable, this));

            } else {
                BetaCraftScreen.INSTANCE.open(this);
            }
        }).pos(this.width / 2 - 50, this.height / 2 - 25 + 20 + 3).size(98, 20);
        if (BetaCraftScreen.SERVER_LIST == null) {
            betaCraftBuilder = betaCraftBuilder.tooltip(Tooltip.create(Component.translatable("betacraft.viafabricplus.warning")));
        }
        this.addRenderableWidget(betaCraftBuilder.build());
    }

    @Override
    public void tick() {
        if (betaCraftButton != null) betaCraftButton.setMessage(Component.nullToEmpty("Loading..."));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTitle(context);
    }

}
