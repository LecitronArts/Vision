package net.burningtnt.accountsx.gui;

import net.burningtnt.accountsx.accounts.AccountType;
import net.burningtnt.accountsx.config.AccountManager;
import net.burningtnt.accountsx.config.AccountWorker;
import net.burningtnt.accountsx.gui.impl.UIScreenImpl;
import net.burningtnt.accountsx.utils.Translator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AccountScreen extends Screen {
    private static final int LAYOUT_HORIZONTAL_SPACING = 16;
    private static final int LAYOUT_VERTICAL_SPACING = 32;

    private static final int LAYOUT_BUTTON_H = 20;

    private static final int LAYOUT_TOOL_BAR_W = 150;
    private static final int LAYOUT_TOOL_BAR_SPACING = 20;
    private static final int LAYOUT_TOOL_BAR_TEXT_CENTER_X = LAYOUT_HORIZONTAL_SPACING + LAYOUT_TOOL_BAR_W / 2;
    private static final int LAYOUT_TOOL_BAR_ADD_ACCOUNT_Y = LAYOUT_VERTICAL_SPACING + LAYOUT_BUTTON_H + LAYOUT_BUTTON_H;

    private static final int LAYOUT_ENTRY_X = LAYOUT_HORIZONTAL_SPACING + LAYOUT_TOOL_BAR_W + LAYOUT_TOOL_BAR_SPACING / 2 + 10;
    private static final int LAYOUT_ENTRY_H = 36;

    private final Component WORKING = Component.translatable("\u6B63\u5728\u64CD\u4F5C...");

    private final Screen parent;
    private AccountListWidget accountListWidget;

    public AccountScreen(Screen parent) {
        super(Component.translatable("\u6240\u6709\u8D26\u6237"));
        this.parent = parent;
    }

    public void close() {
        assert minecraft != null;

        minecraft.setScreen(this.parent);
    }

    public void syncAccounts() {
        this.accountListWidget.syncAccounts();
    }

    @Override
    protected void init() {
        super.init();
        if (this.accountListWidget != null) {
            this.accountListWidget.updateSize(
                    LAYOUT_ENTRY_X, this.width - LAYOUT_HORIZONTAL_SPACING,
                    LAYOUT_VERTICAL_SPACING + 20, this.height - LAYOUT_VERTICAL_SPACING - 20
            );
        } else {
            this.accountListWidget = new AccountListWidget(minecraft,
                    LAYOUT_ENTRY_X, this.width - LAYOUT_HORIZONTAL_SPACING,
                    LAYOUT_VERTICAL_SPACING + 20, this.height - LAYOUT_VERTICAL_SPACING - 20,
                    LAYOUT_ENTRY_H
            );
        }

        this.addWidget(this.accountListWidget);

        this.addField(new ButtonWidget(
                LAYOUT_HORIZONTAL_SPACING, height - LAYOUT_VERTICAL_SPACING - LAYOUT_BUTTON_H - 20,
                LAYOUT_TOOL_BAR_W, LAYOUT_BUTTON_H,
                Component.translatable("\u8FD4\u56DE"),
                button -> this.close())
        );

        int y = LAYOUT_TOOL_BAR_ADD_ACCOUNT_Y;
        for (AccountType type : AccountType.CONFIGURABLE_VALUES) {
            this.addField(new ButtonWidget(
                    LAYOUT_HORIZONTAL_SPACING, y,
                    LAYOUT_TOOL_BAR_W, LAYOUT_BUTTON_H,
                    Translator.translate(type),
                    button -> {
                        assert this.minecraft != null;

                        UIScreenImpl.login(this.minecraft, this, type.getAccountProvider());

                    }
            ));

            y += LAYOUT_BUTTON_H + 2;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.accountListWidget.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(font, AccountWorker.isRunning() ? WORKING : this.title, this.width / 2 + LAYOUT_ENTRY_X / 2, LAYOUT_VERTICAL_SPACING, 0xFFFFFF);
        context.drawCenteredString(font, AccountManager.getCurrentAccountInfoText(), this.width / 2 + LAYOUT_ENTRY_X / 2, this.height - LAYOUT_VERTICAL_SPACING, 0xFFFFFF);

        context.drawCenteredString(
                this.font, Component.translatable("\u6DFB\u52A0\u8D26\u6237"),
                LAYOUT_TOOL_BAR_TEXT_CENTER_X, LAYOUT_TOOL_BAR_ADD_ACCOUNT_Y - 20,
                0xFFFFFF
        );
    }

    public void addField(AbstractWidget drawable) {
        this.addRenderableOnly(drawable);
        this.addWidget(drawable);
    }
}
