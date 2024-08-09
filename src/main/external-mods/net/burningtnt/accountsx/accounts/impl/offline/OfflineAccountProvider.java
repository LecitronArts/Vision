package net.burningtnt.accountsx.accounts.impl.offline;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.burningtnt.accountsx.accounts.AccountProvider;
import net.burningtnt.accountsx.accounts.AccountSession;
import net.burningtnt.accountsx.accounts.AccountUUID;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.burningtnt.accountsx.accounts.gui.Memory;
import net.burningtnt.accountsx.accounts.gui.UIScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class OfflineAccountProvider implements AccountProvider<OfflineAccount> {
    private static final String GUID_PLAYER_NAME_OFFLINE = "guid:\u79BB\u7EBF\u767B\u5F55\u7528\u6237\u540D";
    private static final String GUID_PLAYER_UUID_OFFLINE = "guid:\u79BB\u7EBF\u767B\u5F55 UUID";

    @Override
    public void configure(UIScreen screen) {
        screen.setTitle("\u767B\u9646");
        screen.putTextInput(GUID_PLAYER_NAME_OFFLINE, "\u73A9\u5BB6 ID");
        screen.putTextInput(GUID_PLAYER_UUID_OFFLINE, "\u73A9\u5BB6 UUID");
    }

    @Override
    public int validate(UIScreen screen, Memory memory) throws IllegalArgumentException {
        String playerName = screen.getTextInput(GUID_PLAYER_NAME_OFFLINE);
        memory.set(GUID_PLAYER_NAME_OFFLINE, playerName);

        String playerUUIDString = screen.getTextInput(GUID_PLAYER_UUID_OFFLINE);
        if (playerUUIDString.isEmpty()) {
            memory.set(GUID_PLAYER_UUID_OFFLINE, AccountUUID.generate(playerName));
        } else {
            try {
                memory.set(GUID_PLAYER_UUID_OFFLINE, AccountUUID.parse(playerUUIDString));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot parse current UUID: " + playerUUIDString);
            }
        }

        return STATE_IMMEDIATE_CLOSE;
    }

    @Override
    public OfflineAccount login(Memory memory) {
        return new OfflineAccount(
                UUID.randomUUID().toString().replace("-", ""),
                memory.get(GUID_PLAYER_NAME_OFFLINE, String.class),
                memory.get(GUID_PLAYER_UUID_OFFLINE, UUID.class)
        );
    }

    @Override
    public void refresh(OfflineAccount account) {
        BaseAccount.AccountStorage s = account.getAccountStorage();

        account.setProfile(
                UUID.randomUUID().toString().replace("-", ""),
                s.getPlayerName(),
                s.getPlayerUUID()
        );
    }

    @Override
    public AccountSession createProfile(OfflineAccount account) {
        MinecraftSessionService sessionService = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy(), AccountProvider.createDefaultEnvironment()).createMinecraftSessionService();
        BaseAccount.AccountStorage s = account.getAccountStorage();

        return new AccountSession(AccountProvider.createSession(s), sessionService, UserApiService.OFFLINE);
    }
}
