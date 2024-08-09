package net.burningtnt.accountsx.accounts;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.User;

public final class AccountSession {
    private final User session;

    private final MinecraftSessionService sessionService;

    private final UserApiService userAPIService;

    public AccountSession(User session, MinecraftSessionService sessionService, UserApiService authenticationService) {
        this.session = session;
        this.sessionService = sessionService;
        this.userAPIService = authenticationService;
    }

    public User getSession() {
        return session;
    }

    public MinecraftSessionService getSessionService() {
        return sessionService;
    }

    public UserApiService getUserAPIService() {
        return userAPIService;
    }
}
