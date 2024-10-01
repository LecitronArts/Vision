package dev.vision;

import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.event.LoadCallback;
import dev.tr7zw.entityculling.EntityCullingMod;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.burningtnt.accountsx.AccountsX;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

public class Vision {
    public static Minecraft mc = Minecraft.getInstance();
    public static Vision INSTANCE = new Vision();
    public String CLIENT_NAME = "Vision";
    public String CLIENT_VERSION = "0.1 Preview";
    public String CLIENT_PACKAGE = "dev.vision";
    public Path CLIENT_PATH = mc.gameDirectory.toPath().resolve(CLIENT_NAME);
    public final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);
    public IEventBus EVENT_BUS = new EventBus();
    public void setupClient() {
        try {
            EntityCullingMod.INSTANCE.init();
            AccountsX.INSTANCE.init();
            LoadCallback.EVENT.invoker().onLoad(LoadCallback.State.PRE);
            ViaFabricPlus.global().init();
            LoadCallback.EVENT.invoker().onLoad(LoadCallback.State.POST);
            //see at net.minecraft.client.main.Main
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EVENT_BUS.registerLambdaFactory(CLIENT_PACKAGE , (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        EVENT_BUS.subscribe(this);
        LOGGER.info("Registered event factory.");
    }
}
