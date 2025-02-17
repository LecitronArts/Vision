package net.burningtnt.accountsx.accounts;

import com.mojang.authlib.Environment;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.burningtnt.accountsx.accounts.gui.Memory;
import net.burningtnt.accountsx.accounts.gui.UIScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.util.Optional;

public interface AccountProvider<T extends BaseAccount> {
    int STATE_IMMEDIATE_CLOSE = 0;

    int STATE_HANDLE = 1;

    void configure(UIScreen screen);

    int validate(UIScreen screen, Memory memory) throws IllegalArgumentException;

    T login(Memory memory) throws IOException;

    void refresh(T account) throws IOException;

    default AccountSession createProfile(T account) throws IOException {
        YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy(), createDefaultEnvironment());
        MinecraftSessionService sessionService = service.createMinecraftSessionService();
        BaseAccount.AccountStorage s = account.storage;

        return new AccountSession(
                createSession(s),
                sessionService, service.createUserApiService(s.getAccessToken())
        );
    }

    static Environment createDefaultEnvironment() {
        return new Environment(AuthlibInjectorBreaker.SESSION, AuthlibInjectorBreaker.SERVICES, "PROD");
    }

    static User createSession(BaseAccount.AccountStorage s) {
        return new User(s.getPlayerName(), s.getPlayerUUID(), s.getAccessToken(), Optional.empty(), Optional.empty(), User.Type.MOJANG);
    }

    @SuppressWarnings("unchecked")
    static <T extends BaseAccount> AccountProvider<T> getProvider(T account) {
        return (AccountProvider<T>) account.getAccountType().getAccountProvider();
    }
}
