package net.burningtnt.accountsx.utils;

import net.burningtnt.accountsx.accounts.AccountState;
import net.burningtnt.accountsx.accounts.AccountType;
import net.burningtnt.accountsx.accounts.BaseAccount;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class Translator {

    public static Component translate(AccountType type) {
        return switch (type.name().toLowerCase()) {
            case "env_default" -> Component.literal("\u6E38\u620F\u9ED8\u8BA4\u8D26\u6237");
            case "offline" -> Component.literal("\u79BB\u7EBF\u8D26\u6237");
            case "microsoft" -> Component.literal("\u5FAE\u8F6F\u8D26\u6237");
            case "injector" -> Component.literal("\u5916\u7F6E\u8D26\u6237");
            case "custom" -> Component.literal("\u81EA\u5B9A\u4E49\u8D26\u6237");
            default -> Component.translatable("ERROR while displaying text");
        };
    }

    public static Component translate(AccountState state) {
        return switch (state.name().toLowerCase()) {
            case "unauthorized" -> Component.literal("\u672A\u767B\u5F55");
            case "authorizing" -> Component.literal("\u767B\u5F55\u4E2D");
            case "refreshing" -> Component.literal("\u5237\u65B0\u4E2D");
            case "authorized" -> Component.literal("\u53EF\u7528");
            default -> Component.translatable("ERROR while displaying text");
        };

    }

    public static Component translate(BaseAccount account) {
        return switch (account.getAccountType().name().toLowerCase()) {
            case "env_default" -> Component.literal("\u6B63\u5728\u4F7F\u7528\u9ED8\u8BA4\u8D26\u53F7: " + Minecraft.getInstance().getUser().getName());
            case "offline" -> Component.literal("\u6B63\u5728\u4F7F\u7528\u79BB\u7EBF\u8D26\u53F7: " + Minecraft.getInstance().getUser().getName());
            case "microsoft" -> Component.literal("\u6B63\u5728\u4F7F\u7528\u5FAE\u8F6F\u8D26\u53F7: " + Minecraft.getInstance().getUser().getName());
            case "injector" -> Component.literal("\u6B63\u5728\u4F7F\u7528\u5916\u7F6E\u8D26\u53F7: " + Minecraft.getInstance().getUser().getName());
            case "custom" -> Component.literal("\u6B63\u5728\u4F7F\u7528\u81EA\u5B9A\u4E49\u8D26\u53F7: " + Minecraft.getInstance().getUser().getName());
            default -> Component.translatable("ERROR while displaying text");
        };

    }
}
