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
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ClassiCubeMFAScreen extends VFPScreen {

    public static final ClassiCubeMFAScreen INSTANCE = new ClassiCubeMFAScreen();

    public ClassiCubeMFAScreen() {
        super("ClassiCube MFA", false);
    }

    private EditBox mfaField;

    @Override
    protected void init() {
        super.init();
        this.setupSubtitle(Component.translatable("classic4j_library.viafabricplus.error.logincode"));

        this.addRenderableWidget(mfaField = new EditBox(font, width / 2 - 150, 70 + 10, 300, 20, Component.empty()));

        mfaField.setHint(Component.literal("MFA"));

        this.addRenderableWidget(Button.builder(Component.literal("Login"), button -> {
            this.setupSubtitle(Component.translatable("classicube.viafabricplus.loading"));
            final CCAccount account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getClassicubeAccount();

            ClassiCubeHandler.requestAuthentication(account, mfaField.getValue(), new LoginProcessHandler() {
                @Override
                public void handleMfa(CCAccount account) {
                    // Not implemented in this case
                }

                @Override
                public void handleSuccessfulLogin(CCAccount account) {
                    RenderSystem.recordRenderCall(() -> ClassiCubeServerListScreen.open(prevScreen, this));
                }

                @Override
                public void handleException(Throwable throwable) {
                    setupSubtitle(Component.literal(throwable.getMessage()));
                }
            });
        }).pos(width / 2 - 75, mfaField.getY() + (20 * 4) + 5).size(150, 20).build());
    }

    @Override
    public void onClose() {
        // The user wasn't logged in when opening this screen, so he cancelled the login process, so we can safely unset the account
        ViaFabricPlus.global().getSaveManager().getAccountsSave().setClassicubeAccount(null);
        ProtocolSelectionScreen.INSTANCE.open(prevScreen);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTitle(context);

        context.drawCenteredString(this.font, this.title, this.width / 2, 70, 16777215);
    }

}
