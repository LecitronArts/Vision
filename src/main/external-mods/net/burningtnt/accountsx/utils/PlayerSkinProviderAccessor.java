package net.burningtnt.accountsx.utils;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public interface PlayerSkinProviderAccessor {
    Path accountx$getDirectory();

    Executor accountx$getExecutor();
}
