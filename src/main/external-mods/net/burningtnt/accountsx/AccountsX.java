package net.burningtnt.accountsx;

import net.burningtnt.accountsx.config.AccountManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;

import java.io.IOException;

public class AccountsX {
    public static AccountsX INSTANCE = new AccountsX();
    public final Logger LOGGER = LogManager.getLogger();
    public final String MOD_NAME = "Accounts X";

    public void init() throws IOException {
        LOGGER.info("[" + MOD_NAME + "] Initializing...");
        AccountManager.initialize();
    }
}
