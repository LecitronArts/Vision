package net.burningtnt.accountsx.accounts.impl.env;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.burningtnt.accountsx.AccountsX;
import net.burningtnt.accountsx.accounts.AccountProvider;
import net.burningtnt.accountsx.accounts.AccountSession;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.burningtnt.accountsx.accounts.gui.Memory;
import net.burningtnt.accountsx.accounts.gui.UIScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import java.io.IOException;

public final class EnvironmentAccountProvider implements AccountProvider<EnvironmentAccount> {
    public static BaseAccount fromSession(User session) {
        return new EnvironmentAccount(session.getAccessToken(), session.getName(), session.getProfileId());
    }

    @Override
    public void configure(UIScreen screen) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int validate(UIScreen screen, Memory memory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnvironmentAccount login(Memory memory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(EnvironmentAccount account) {
    }

    @Override
    public AccountSession createProfile(EnvironmentAccount account) {
        try {
            return AccountProvider.super.createProfile(account);
        } catch (IOException e) {
            AccountsX.INSTANCE.LOGGER.warn("Cannot authorize the environment account. Fallback to Offline Mode.");

            MinecraftSessionService sessionService = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy()).createMinecraftSessionService();
            BaseAccount.AccountStorage s = account.getAccountStorage();

            return new AccountSession(AccountProvider.createSession(s), sessionService, UserApiService.OFFLINE);
        }
    }
}
