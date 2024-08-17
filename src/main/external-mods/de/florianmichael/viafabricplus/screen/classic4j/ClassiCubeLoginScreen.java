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
import de.florianmichael.classic4j.model.classicube.account.CCAccount;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.save.impl.AccountsSave;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;

public class ClassiCubeLoginScreen extends VFPScreen {

    public static final ClassiCubeLoginScreen INSTANCE = new ClassiCubeLoginScreen();

    public ClassiCubeLoginScreen() {
        super("ClassiCube Login", true);
    }

    private EditBox nameField;
    private EditBox passwordField;

    @Override
    protected void init() {
        super.init();
        this.setupSubtitle(Component.translatable("classicube.viafabricplus.account"), ConfirmLinkScreen.confirmLink(this, ClassiCubeHandler.CLASSICUBE_ROOT_URI.toString()));

        this.addRenderableWidget(nameField = new EditBox(font, width / 2 - 150, 70 + 10, 300, 20, Component.empty()));
        this.addRenderableWidget(passwordField = new EditBox(font, width / 2 - 150, nameField.getY() + 20 + 5, 300, 20, Component.empty()));
        passwordField.setFormatter((s, integer) -> Component.literal("*".repeat(s.length())).getVisualOrderText());

        nameField.setHint(Component.literal("Name"));
        passwordField.setHint(Component.literal("Password"));

        nameField.setMaxLength(Integer.MAX_VALUE);
        passwordField.setMaxLength(Integer.MAX_VALUE);

        ( nameField).viaFabricPlus$unlockForbiddenCharacters();
        ( passwordField).viaFabricPlus$unlockForbiddenCharacters();

        final AccountsSave accountsSave = ViaFabricPlus.global().getSaveManager().getAccountsSave();
        if (accountsSave.getClassicubeAccount() != null) {
            nameField.setValue(accountsSave.getClassicubeAccount().username());
            passwordField.setValue(accountsSave.getClassicubeAccount().username());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Login"), button -> {
            accountsSave.setClassicubeAccount(new CCAccount(nameField.getValue(), passwordField.getValue()));
            this.setupSubtitle(Component.translatable("classicube.viafabricplus.loading"));

            ClassiCubeHandler.requestAuthentication(accountsSave.getClassicubeAccount(), null, new LoginProcessHandler() {

                @Override
                public void handleMfa(CCAccount account) {
                    ClassiCubeMFAScreen.INSTANCE.open(prevScreen);
                }

                @Override
                public void handleSuccessfulLogin(CCAccount account) {
                    RenderSystem.recordRenderCall(() -> ClassiCubeServerListScreen.open(prevScreen, this));
                }

                @Override
                public void handleException(Throwable throwable) {
                    ViaFabricPlus.global().getLogger().error("Error while logging in to ClassiCube!", throwable);
                    setupSubtitle(Component.literal(throwable.getMessage()));
                }
            });
        }).pos(width / 2 - 75, passwordField.getY() + (20 * 4) + 5).size(150, 20).build());
    }

    @Override
    public void onClose() {
        // The user wasn't logged in when opening this screen, so he cancelled the login process, so we can safely unset the account
        ViaFabricPlus.global().getSaveManager().getAccountsSave().setClassicubeAccount(null);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTitle(context);

        context.drawCenteredString(this.font, this.title, this.width / 2, 70, 16777215);
    }

}
