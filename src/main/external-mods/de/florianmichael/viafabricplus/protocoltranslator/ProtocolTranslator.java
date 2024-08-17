/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.protocoltranslator;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import de.florianmichael.viafabricplus.event.ChangeProtocolVersionCallback;
import de.florianmichael.viafabricplus.event.PostViaVersionLoadCallback;
import de.florianmichael.viafabricplus.protocoltranslator.impl.command.ViaFabricPlusVLCommandHandler;
import de.florianmichael.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaLegacyPlatformImpl;
import de.florianmichael.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaVersionPlatformImpl;
import de.florianmichael.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLInjector;
import de.florianmichael.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLLoader;
import de.florianmichael.viafabricplus.protocoltranslator.netty.ViaFabricPlusVLLegacyPipeline;
import de.florianmichael.viafabricplus.protocoltranslator.util.NoPacketSendChannel;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.ViaAprilFoolsPlatformImpl;
import net.raphimc.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import net.raphimc.vialoader.impl.platform.ViaBedrockPlatformImpl;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class represents the whole Protocol Translator, here all important variables are stored
 */
public class ProtocolTranslator {

    /**
     * These attribute keys are used to track the main connections of Minecraft and ViaVersion, so that they can be used later during the connection to sendWhen packets.
     */
    public static final AttributeKey<Connection> CLIENT_CONNECTION_ATTRIBUTE_KEY = AttributeKey.newInstance("viafabricplus-clientconnection");

    /**
     * This attribute stores the forced version for the current connection (if you set a specific version in the Edit Server screen)
     */
    public static final AttributeKey<ProtocolVersion> TARGET_VERSION_ATTRIBUTE_KEY = AttributeKey.newInstance("viafabricplus-targetversion");

    /**
     * The native version of the client
     */
    public static final ProtocolVersion NATIVE_VERSION = ProtocolVersion.v1_20_3;

    /**
     * Protocol version that is used to enable protocol auto-detect
     */
    public static final ProtocolVersion AUTO_DETECT_PROTOCOL = new ProtocolVersion(VersionType.SPECIAL, -2, -1, "Auto Detect (1.7+ servers)", null) {
        @Override
        protected Comparator<ProtocolVersion> customComparator() {
            return (o1, o2) -> {
                if (o1 == AUTO_DETECT_PROTOCOL) {
                    return 1;
                } else if (o2 == AUTO_DETECT_PROTOCOL) {
                    return -1;
                } else {
                    return 0;
                }
            };
        }

        @Override
        public boolean isKnown() {
            return false;
        }
    };

    /**
     * This field stores the target version that you set in the GUI
     */
    private static ProtocolVersion targetVersion = NATIVE_VERSION;

    /**
     * This field stores the previous selected version if {@link #setTargetVersion(ProtocolVersion, boolean)} is called with revertOnDisconnect set to true
     */
    private static ProtocolVersion previousVersion = null;

    /**
     * Injects the ViaFabricPlus pipeline with all ViaVersion elements into a Minecraft pipeline
     *
     * @param connection the Minecraft connection
     */
    public static void injectViaPipeline(final Connection connection, final Channel channel) {
        final ProtocolVersion serverVersion = connection.viaFabricPlus$getTargetVersion();

        if (serverVersion != ProtocolTranslator.NATIVE_VERSION) {
            channel.attr(ProtocolTranslator.CLIENT_CONNECTION_ATTRIBUTE_KEY).set(connection);
            channel.attr(ProtocolTranslator.TARGET_VERSION_ATTRIBUTE_KEY).set(serverVersion);

            if (serverVersion.equals(BedrockProtocolVersion.bedrockLatest)) {
                channel.config().setOption(RakChannelOption.RAK_PROTOCOL_VERSION, ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION);
                channel.config().setOption(RakChannelOption.RAK_CONNECT_TIMEOUT, 4_000L);
                channel.config().setOption(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L);
                channel.config().setOption(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
            }

            final UserConnection user = new UserConnectionImpl(channel, true);
            new ProtocolPipelineImpl(user);
            connection.viaFabricPlus$setUserConnection(user);

            channel.pipeline().addLast(new ViaFabricPlusVLLegacyPipeline(user, serverVersion));
        }
    }

    /**
     * This method is used when you need the target version after connecting to the server.
     *
     * @return the target version
     */
    public static ProtocolVersion getTargetVersion() {
        return targetVersion;
    }

    /**
     * Sets the target version
     *
     * @param newVersion the target version
     */
    public static void setTargetVersion(final ProtocolVersion newVersion) {
        setTargetVersion(newVersion, false);
    }

    /**
     * Sets the target version
     *
     * @param newVersion         the target version
     * @param revertOnDisconnect if true, the previous version will be set when the player disconnects from the server
     */
    public static void setTargetVersion(final ProtocolVersion newVersion, final boolean revertOnDisconnect) {
        if (newVersion == null) return;

        final ProtocolVersion oldVersion = targetVersion;
        targetVersion = newVersion;
        if (oldVersion != newVersion) {
            if (revertOnDisconnect) {
                previousVersion = oldVersion;
            }
            ChangeProtocolVersionCallback.EVENT.invoker().onChangeProtocolVersion(oldVersion, targetVersion);
        }
    }

    /**
     * Resets the previous version if it is set. Calling {@link #setTargetVersion(ProtocolVersion, boolean)} with revertOnDisconnect set to true will set it.
     */
    public static void injectPreviousVersionReset(final Channel channel) {
        if (previousVersion == null) return;

        channel.closeFuture().addListener(future -> {
            setTargetVersion(previousVersion);
            previousVersion = null;
        });
    }

    /**
     * @param clientVersion The client version
     * @param serverVersion The server version
     * @return Creates a dummy UserConnection class with a valid protocol pipeline to emulate packets
     */
    public static UserConnection createDummyUserConnection(final ProtocolVersion clientVersion, final ProtocolVersion serverVersion) {
        final UserConnection user = new UserConnectionImpl(NoPacketSendChannel.INSTANCE, true);
        final ProtocolPipeline pipeline = new ProtocolPipelineImpl(user);
        final List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
        if (path != null) {
            for (ProtocolPathEntry pair : path) {
                pipeline.add(pair.protocol());
                pair.protocol().init(user);
            }
        }

        final ProtocolInfo info = user.getProtocolInfo();
        info.setState(State.PLAY);
        info.setProtocolVersion(clientVersion);
        info.setServerProtocolVersion(serverVersion);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            final GameProfile profile = mc.player.getGameProfile();
            info.setUsername(profile.getName());
            info.setUuid(profile.getId());
        }

        return user;
    }

    /**
     * @return Returns the current UserConnection of the connection to the server, if the player isn't connected to a server it will return null
     * @throws IllegalStateException If the player is not connected to a server
     */
    public static UserConnection getPlayNetworkUserConnection() {
        final ClientPacketListener handler = Minecraft.getInstance().getConnection();
        if (handler == null) {
            throw new IllegalStateException("The player is not connected to a server");
        }

        return ( handler.getConnection()).viaFabricPlus$getUserConnection();
    }

    /**
     * Apply recommended config options to the ViaVersion config files
     *
     * @param configFolder The directory where the ViaVersion config files are located
     */
    public static void patchConfigs(final File configFolder) {
        configFolder.mkdirs();

        try {
            final File viaVersionConfig = new File(configFolder, "viaversion.yml");
            Files.writeString(viaVersionConfig.toPath(), """
                    fix-infested-block-breaking: false
                    shield-blocking: false
                    no-delay-shield-blocking: true
                    chunk-border-fix: true
                    """, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaVersion config", e);
        }

        try {
            final File viaLegacyConfig = new File(configFolder, "vialegacy.yml");
            Files.writeString(viaLegacyConfig.toPath(), """
                    legacy-skull-loading: true
                    legacy-skin-loading: true
                    """, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaLegacy config", e);
        }
    }

    /**
     * Returns true if first is closer to version than second
     *
     * @param version The version to compare to
     * @param first   The first version
     * @param second  The second version
     * @return true if first is closer to version than second
     */
    public static boolean isCloserTo(final ProtocolVersion version, final ProtocolVersion first, final ProtocolVersion second) {
        if (version.getVersionType() == first.getVersionType() || version.getVersionType() == second.getVersionType()) {
            return Math.abs(version.getVersion() - first.getVersion()) < Math.abs(version.getVersion() - second.getVersion());
        } else {
            final int ordinal = version.getVersionType().ordinal();
            return Math.abs(ordinal - first.getVersionType().ordinal()) < Math.abs(ordinal - second.getVersionType().ordinal());
        }
    }

    /**
     * This method is used to initialize the whole Protocol Translator
     *
     * @param directory The directory where the ViaVersion config files are located
     * @return A CompletableFuture that will be completed when the initialization is done
     */
    public static CompletableFuture<Void> init(final File directory) {
        patchConfigs(new File(directory, "ViaLoader"));

        // Register command callback for /viafabricplus
        /*ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            final var commandHandler = (ViaFabricPlusVLCommandHandler) Via.getManager().getCommandHandler();
            final var executor = RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("args", StringArgumentType.greedyString()).executes(commandHandler::execute).suggests(commandHandler::suggestion);

            dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("viafabricplus").then(executor).executes(commandHandler::execute));
        });

         */

        return CompletableFuture.runAsync(() -> {
            // Load ViaVersion and register all platforms and their components
            ViaLoader.init(
                    new ViaFabricPlusViaVersionPlatformImpl(directory),
                    new ViaFabricPlusVLLoader(),
                    new ViaFabricPlusVLInjector(),
                    new ViaFabricPlusVLCommandHandler(),

                    ViaBackwardsPlatformImpl::new,
                    ViaFabricPlusViaLegacyPlatformImpl::new,
                    ViaAprilFoolsPlatformImpl::new,
                    ViaBedrockPlatformImpl::new
            );
            ProtocolVersion.register(AUTO_DETECT_PROTOCOL);
            PostViaVersionLoadCallback.EVENT.invoker().onPostViaVersionLoad();
        });
    }

}
