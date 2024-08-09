package net.burningtnt.accountsx.accounts.impl.injector;

import com.google.gson.JsonObject;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.burningtnt.accountsx.accounts.AccountProvider;
import net.burningtnt.accountsx.accounts.AccountSession;
import net.burningtnt.accountsx.accounts.AccountUUID;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.burningtnt.accountsx.accounts.gui.Memory;
import net.burningtnt.accountsx.accounts.gui.UIScreen;
import net.burningtnt.accountsx.accounts.impl.injector.service.LocalYggdrasilMinecraftSessionService;
import net.burningtnt.accountsx.utils.IOUtils;
import net.minecraft.client.Minecraft;

import java.io.IOException;

public class InjectorAccountProvider implements AccountProvider<InjectorAccount> {
    private static final String GUID_SERVER_DOMAIN_INJECT = "guid:\u5916\u7F6E\u767B\u5F55\u7F51\u5740";
    private static final String GUID_USER_NAME_INJECT = "guid:\u5916\u7F6E\u767B\u5F55\u7528\u6237\u540D";
    private static final String GUID_USER_UUID_INJECT = "guid:\u5916\u7F6E\u767B\u5F55\u5BC6\u7801";

    @Override
    public void configure(UIScreen screen) {
        screen.setTitle("\u767B\u9646");
        screen.putTextInput(GUID_SERVER_DOMAIN_INJECT, "\u8BA4\u8BC1\u670D\u52A1\u5668\uFF08\u4EC5\u6839\u57DF\u540D\uFF09");
        screen.putTextInput(GUID_USER_NAME_INJECT, "\u7528\u6237 ID");
        screen.putTextInput(GUID_USER_UUID_INJECT, "\u7528\u6237\u5BC6\u7801");
    }

    @Override
    public int validate(UIScreen screen, Memory memory) throws IllegalArgumentException {
        memory.set(GUID_SERVER_DOMAIN_INJECT, screen.getTextInput(GUID_SERVER_DOMAIN_INJECT));
        memory.set(GUID_USER_NAME_INJECT, screen.getTextInput(GUID_USER_NAME_INJECT));
        memory.set(GUID_USER_UUID_INJECT, screen.getTextInput(GUID_USER_UUID_INJECT));

        return STATE_IMMEDIATE_CLOSE;
    }

    @Override
    public InjectorAccount login(Memory memory) throws IOException {
        String url = "https://" + memory.get(GUID_SERVER_DOMAIN_INJECT, String.class) + "/api/yggdrasil/authserver/authenticate";

        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);

        JsonObject root = new JsonObject();
        root.add("agent", agent);
        root.addProperty("username", memory.get(GUID_USER_NAME_INJECT, String.class));
        root.addProperty("password", memory.get(GUID_USER_UUID_INJECT, String.class));

        JsonObject json = IOUtils.postRequest(url, root);
        if (json.has("error")) {
            throw new IOException("Cannot auth this injector: " + json.get("errorMessage").getAsString());
        }

        String accessToken = json.get("accessToken").getAsString();
        String playerUUID = json.get("selectedProfile").getAsJsonObject().get("id").getAsString();
        String playerName = json.get("selectedProfile").getAsJsonObject().get("name").getAsString();


        return new InjectorAccount(
                accessToken, playerName, AccountUUID.parse(playerUUID),
                memory.get(GUID_SERVER_DOMAIN_INJECT, String.class), memory.get(GUID_USER_NAME_INJECT, String.class), memory.get(GUID_USER_UUID_INJECT, String.class)
        );
    }

    @Override
    public void refresh(InjectorAccount account) throws IOException {
        String url = "https://" + account.getServer() + "/api/yggdrasil/authserver/authenticate";

        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);

        JsonObject root = new JsonObject();
        root.add("agent", agent);
        root.addProperty("username", account.getUserName());
        root.addProperty("password", account.getPassword());

        JsonObject json = IOUtils.postRequest(url, root);
        if (json.has("error")) {
            throw new IOException("Cannot auth this injector: " + json.get("errorMessage").getAsString());
        }

        String accessToken = json.get("accessToken").getAsString();
        String playerUUID = json.get("selectedProfile").getAsJsonObject().get("id").getAsString();
        String playerName = json.get("selectedProfile").getAsJsonObject().get("name").getAsString();

        account.setProfile(accessToken, playerName, AccountUUID.parse(playerUUID));
    }

    @Override
    public AccountSession createProfile(InjectorAccount account) {
        String url = account.getServer();
        Environment env = new Environment(
                "https://" + url + "/api/yggdrasil/sessionserver",
                "https://" + url + "/api/yggdrasil/minecraftservices",
                "Authlib-Injector"
        );

        YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy(), env);
        BaseAccount.AccountStorage s = account.getAccountStorage();
        LocalYggdrasilMinecraftSessionService sessionService = new LocalYggdrasilMinecraftSessionService(authenticationService, env);

        return new AccountSession(AccountProvider.createSession(s), sessionService, authenticationService.createUserApiService(s.getAccessToken()));
    }
}
