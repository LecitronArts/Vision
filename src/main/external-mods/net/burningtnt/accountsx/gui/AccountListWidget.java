package net.burningtnt.accountsx.gui;

import net.burningtnt.accountsx.accounts.AccountProvider;
import net.burningtnt.accountsx.accounts.AccountSession;
import net.burningtnt.accountsx.accounts.AccountType;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.burningtnt.accountsx.config.AccountManager;
import net.burningtnt.accountsx.config.AccountWorker;
import net.burningtnt.accountsx.utils.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class AccountListWidget extends ObjectSelectionList<AccountListWidget.AccountEntry> {
    public AccountListWidget(Minecraft client, int left, int right, int top, int bottom, int entryHeight) {
        super(client, right - left, bottom - top, top, entryHeight);
        this.updateSize(left, right, top, bottom);

        syncAccounts();
    }

    public void updateSize(int left, int right, int top, int bottom) {
        this.setX(left);
        this.setY(top);
        this.setWidth(right - left);
        this.setHeight(bottom - top);
    }

    public void syncAccounts() {
        this.clearEntries();
        for (BaseAccount account : AccountManager.getAccounts()) {
            AccountEntry entry = new AccountEntry(account);
            this.addEntry(entry);

            if (account == AccountManager.getCurrentAccount()) {
                super.setSelected(entry);
            }
        }
    }

    @Override
    public void setSelected(@Nullable AccountListWidget.AccountEntry entry) {
        if (entry != null) {
            if (AccountManager.getCurrentAccount() != entry.account) {
                AccountWorker.submit(() -> {
                    if (AccountManager.getCurrentAccount() == entry.account) {
                        return;
                    }

                    AccountSession session = AccountProvider.getProvider(entry.account).createProfile(entry.account);

                    minecraft.tell(() -> {
                        AccountManager.switchAccount(entry.account, session);

                        super.setSelected(entry);
                        minecraft.getNarrator().say((Component.translatable("narrator.select", entry.account.getAccountStorage().getPlayerName())));
                    });
                });
            }
        } else {
            super.setSelected(null);
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        AccountEntry entry = this.getSelected();
        return entry != null && entry.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    public class AccountEntry extends Entry<AccountEntry> {
        private static final String ACTION_UP = "\u2191";

        private static final String ACTION_DELETE = "x";

        private static final String ACTION_DOWN = "\u2193";

        private final BaseAccount account;

        public AccountEntry(BaseAccount account) {
            this.account = account;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawString(minecraft.font, this.account.getAccountStorage().getPlayerName(), x + 32 + 3, y + 1, 0xFFFFFF, false);
            context.drawString(minecraft.font, Translator.translate(this.account.getAccountType()), x + 32 + 3, y + 1 + 9, 0xFFFFFF, false);
            context.drawString(minecraft.font, Translator.translate(this.account.getAccountState()), x + 32 + 3, y + 1 + 18, 0xFFFFFF, false);

            if (this.account.getAccountType() != AccountType.ENV_DEFAULT) {
                if (index > 1) {
                    minecraft.font.drawInBatch(ACTION_UP, (float) (x + entryWidth - 1.5 * minecraft.font.width(ACTION_UP)), (float) (y + 1 + 5 - minecraft.font.lineHeight / 2), 0xFFFFFF, false, context.pose().last().pose(), context.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
                }

                minecraft.font.drawInBatch(ACTION_DELETE, (float) (x + entryWidth - 1.5 * minecraft.font.width(ACTION_DELETE)), (float) (y + 1 + 15 - minecraft.font.lineHeight / 2), 0xFFFFFF, false, context.pose().last().pose(), context.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);

                if (index < getItemCount() - 1) {
                    minecraft.font.drawInBatch(ACTION_DOWN, (float) (x + entryWidth - 1.5 * minecraft.font.width(ACTION_DOWN)), (float) (y + 1 + 25 - minecraft.font.lineHeight / 2), 0xFFFFFF, false, context.pose().last().pose(), context.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int right = getRowRight();
            int buttonW = minecraft.font.width("x");
            if (mouseX >= right - buttonW * 1.5 && mouseX <= right - buttonW * 0.5) {
                int index = children().indexOf(this);
                int top = getRowTop(index);

                if (this.account.getAccountType() != AccountType.ENV_DEFAULT) {
                    if (index > 1) {
                        int btnTop = top + 1 + 5 - minecraft.font.lineHeight / 2;
                        if (mouseY >= btnTop && mouseY <= btnTop + minecraft.font.lineHeight) {
                            AccountManager.moveAccount(this.account, index - 1);
                            AccountListWidget.this.syncAccounts();
                            return false;
                        }
                    }

                    int btnTop = top + 1 + 15 - minecraft.font.lineHeight / 2;
                    if (mouseY >= btnTop && mouseY <= btnTop + minecraft.font.lineHeight) {
                        AccountManager.dropAccount(this.account);
                        AccountListWidget.this.syncAccounts();
                        return false;
                    }

                    if (index < getItemCount() - 1) {
                        int btnTop2 = top + 1 + 25 - minecraft.font.lineHeight / 2;
                        if (mouseY >= btnTop2 && mouseY <= btnTop2 + minecraft.font.lineHeight) {
                            AccountManager.moveAccount(this.account, index + 1);
                            AccountListWidget.this.syncAccounts();
                            return false;
                        }
                    }
                }
            }

            setSelected(this);
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal("");
        }
    }
}
