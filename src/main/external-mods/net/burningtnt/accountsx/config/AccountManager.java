package net.burningtnt.accountsx.config;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import net.burningtnt.accountsx.accounts.*;
import net.burningtnt.accountsx.accounts.impl.env.EnvironmentAccountProvider;
import net.burningtnt.accountsx.utils.PlayerSkinProviderAccessor;
import net.burningtnt.accountsx.utils.Translator;
import net.burningtnt.accountsx.utils.threading.ThreadState;
import net.burningtnt.accountsx.utils.threading.Threading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AccountManager {
    private static final List<BaseAccount> accounts = new CopyOnWriteArrayList<>();
    private static final List<BaseAccount> readonlyAccounts = Collections.unmodifiableList(accounts);
    private static volatile BaseAccount current = null;

    private AccountManager() {
    }

    public static List<BaseAccount> getAccounts() {
        return readonlyAccounts;
    }

    public static BaseAccount getCurrentAccount() {
        return current;
    }

    public static void initialize() {
        BaseAccount defaultAccount = EnvironmentAccountProvider.fromSession(Minecraft.getInstance().getUser());
        accounts.add(defaultAccount);
        current = defaultAccount;

        accounts.addAll(ConfigHandle.load());

        for (BaseAccount account : accounts) {
            if (account.getAccountState() != AccountState.AUTHORIZED) {
                AccountWorker.submit(() -> {
                    account.setProfileState(AccountState.AUTHORIZING);

                    try {
                        AccountProvider.getProvider(account).refresh(account);
                    } catch (Throwable t) {
                        account.setProfileState(AccountState.UNAUTHORIZED);
                    }
                });
            }
        }
    }

    @ThreadState("Minecraft Client Thread")
    public static void dropAccount(BaseAccount account) {
        Threading.checkMinecraftClientThread();

        if (account.getAccountType() == AccountType.ENV_DEFAULT) {
            return;
        }

        accounts.remove(account);
        AccountWorker.submit(ConfigHandle::write);
    }

    @ThreadState("Minecraft Client Thread")
    public static void addAccount(BaseAccount account) {
        Threading.checkMinecraftClientThread();

        accounts.add(account);
        AccountWorker.submit(ConfigHandle::write);
    }

    @ThreadState("Minecraft Client Thread")
    public static void moveAccount(BaseAccount account, int index) {
        Threading.checkMinecraftClientThread();

        accounts.remove(account);
        accounts.add(index, account);
        AccountWorker.submit(ConfigHandle::write);
    }

    @ThreadState("Minecraft Client Thread")
    public static <T extends BaseAccount> void switchAccount(T account, AccountSession service) {
        Threading.checkMinecraftClientThread();

        if (account.getAccountState() != AccountState.AUTHORIZED) {
            throw new IllegalStateException("Account is not authorized.");
        }

        Minecraft client = Minecraft.getInstance();
        current = account;

        MinecraftSessionService sessionService = service.getSessionService();
        UserApiService userAPIService = service.getUserAPIService();

        client.setUser(service.getSession());
        client.setSessionService(sessionService);
        client.setUserApiService(userAPIService);
        client.setPlayerSocialManager(new PlayerSocialManager(client, userAPIService));
        client.setSkinManager(new SkinManager(
                client.getTextureManager(),
                ((PlayerSkinProviderAccessor) client.getSkinManager()).accountx$getDirectory(),
                sessionService,
                ((PlayerSkinProviderAccessor) client.getSkinManager()).accountx$getExecutor()
        ));
    }

    public static Component getCurrentAccountInfoText() {
        return Translator.translate(current);
    }
}
