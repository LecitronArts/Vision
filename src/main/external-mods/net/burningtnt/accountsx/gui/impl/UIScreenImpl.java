package net.burningtnt.accountsx.gui.impl;

import dev.vision.Vision;
import net.burningtnt.accountsx.AccountsX;
import net.burningtnt.accountsx.accounts.AccountProvider;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.burningtnt.accountsx.accounts.gui.Memory;
import net.burningtnt.accountsx.accounts.gui.UIScreen;
import net.burningtnt.accountsx.config.AccountManager;
import net.burningtnt.accountsx.config.AccountWorker;
import net.burningtnt.accountsx.gui.AccountScreen;
import net.burningtnt.accountsx.gui.ButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UIScreenImpl implements UIScreen {
    public static void login(Minecraft client, AccountScreen accountScreen, AccountProvider<?> provider) {
        UIScreenImpl screen = new UIScreenImpl();
        provider.configure(screen);

        client.setScreen(screen.bind(accountScreen, provider));
    }

    private static final class ValuedWidget<T extends Renderable> {
        private final String description;

        private T widget;

        public ValuedWidget(String description) {
            this.description = description;
        }
    }

    private boolean readonly = false;

    private String title;

    private final Map<String, ValuedWidget<EditBox>> inputs = new LinkedHashMap<>();

    @Override
    public void setTitle(String description) {
        if (readonly) {
            throw new IllegalStateException("UIScreen has been frozen.");
        }
        this.title = description;
    }

    @Override
    public void putTextInput(String guid, String description) {
        if (readonly) {
            throw new IllegalStateException("UIScreen has been frozen.");
        }
        this.inputs.put(guid, new ValuedWidget<>(description));
    }

    @Override
    public String getTextInput(String guid) {
        if (!readonly) {
            throw new IllegalStateException("UIScreen hasn't been frozen.");
        }
        return this.inputs.get(guid).widget.getValue();
    }

    public Screen bind(AccountScreen parent, AccountProvider<?> provider) {
        readonly = true;
        return new LoginScreen(Component.translatable(this.title), parent, provider);
    }

    private final class LoginScreen extends Screen {
        private final AccountScreen parent;

        private final AccountProvider<?> provider;

        public LoginScreen(Component title, AccountScreen parent, AccountProvider<?> provider) {
            super(title);
            this.parent = parent;
            this.provider = provider;
        }

        public void close() {
            assert this.minecraft != null;

            this.minecraft.setScreen(parent);
        }

        @Override
        protected void init() {
            assert this.minecraft != null;

            super.init();

            int widgetsTop = this.height / 2 - (UIScreenImpl.this.inputs.size() + 1) * 25 / 2;
            int widgetsLeft = this.width / 2 - 50;

            for (ValuedWidget<EditBox> widget : UIScreenImpl.this.inputs.values()) {
                widget.widget = this.addField(
                        new EditBox(this.minecraft.font, widgetsLeft, widgetsTop, 200, 20, Component.empty())
                );

                widgetsTop += 25;
            }

            boolean noInputs = UIScreenImpl.this.inputs.isEmpty();

            this.addField(new ButtonWidget(widgetsLeft, widgetsTop, noInputs ? 100 : 95, 20, Component.translatable("\u767B\u9646"), widget -> {
                Memory memory = new DefaultMemory(this);

                int state;
                try {
                    state = this.provider.validate(UIScreenImpl.this, memory);
                } catch (IllegalArgumentException e) {
                    AccountsX.INSTANCE.LOGGER.warn("Invalid account argument.", e);
                    return;
                }

                switch (state) {
                    case AccountProvider.STATE_IMMEDIATE_CLOSE -> this.close();
                    case AccountProvider.STATE_HANDLE -> {
                    }
                    default -> throw new IllegalArgumentException("Unknown state: " + state);
                }

                AccountWorker.submit(() -> {
                    BaseAccount account;
                    try {
                        account = this.provider.login(memory);
                    } catch (Throwable t) {
                        this.minecraft.tell(() -> {
                            if (this.minecraft.screen == this) {
                                this.close();
                            }
                        });

                        throw t;
                    }

                    this.minecraft.tell(() -> {
                        if (this.minecraft.screen == this) {
                            this.close();
                        }

                        AccountManager.addAccount(account);

                        this.parent.syncAccounts();
                    });
                });
            }));

            if (noInputs) {
                this.addField(new ButtonWidget(widgetsLeft, widgetsTop + 25, 100, 20, Component.translatable("\u8FD4\u56DE"), widget -> this.close()));
            } else {
                this.addField(new ButtonWidget(widgetsLeft + 105, widgetsTop, 95, 20, Component.translatable("\u8FD4\u56DE"), widget -> this.close()));
            }
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            assert this.minecraft != null;

            super.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);

            int textTop = this.height / 2 - (UIScreenImpl.this.inputs.size() + 1) * 25 / 2 + 5;
            int textLeft = this.width / 2 - 170;

            minecraft.font.drawInBatch(
                    this.title,
                    (float) this.width / 2 - (float) minecraft.font.width(this.title) / 2, textTop - 40,
                    0xFFFFFF, true,
                    context.pose().last().pose(), context.bufferSource(), Font.DisplayMode.NORMAL,
                    0, 0xF000F0
            );

            for (ValuedWidget<EditBox> widget : UIScreenImpl.this.inputs.values()) {
                minecraft.font.drawInBatch(
                        Component.translatable(widget.description), textLeft, textTop, 0xFFFFFF, true,
                        context.pose().last().pose(), context.bufferSource(), Font.DisplayMode.NORMAL,
                        0, 0xF000F0
                );

                textTop += 25;
            }
        }

        public <T extends AbstractWidget> T addField(T drawable) {
            this.addRenderableOnly(drawable);
            this.addWidget(drawable);
            return drawable;
        }
    }
}
