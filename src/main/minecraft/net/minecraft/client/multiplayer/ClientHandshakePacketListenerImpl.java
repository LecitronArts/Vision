package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;


import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.util.Crypt;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.ProtocolMetadataStorage;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private final ServerData serverData;
   @Nullable
   private final Screen parent;
   private final Consumer<Component> updateStatus;
   private final Connection connection;
   private final boolean newWorld;
   @Nullable
   private final Duration worldLoadDuration;
   @Nullable
   private String minigameName;
   private final AtomicReference<ClientHandshakePacketListenerImpl.State> state = new AtomicReference<>(ClientHandshakePacketListenerImpl.State.CONNECTING);

   public ClientHandshakePacketListenerImpl(Connection pConnection, Minecraft pMinecraft, @Nullable ServerData pServerData, @Nullable Screen pParent, boolean pNewWorld, @Nullable Duration pWorldLoadDuration, Consumer<Component> pUpdateStatus) {
      this.connection = pConnection;
      this.minecraft = pMinecraft;
      this.serverData = pServerData;
      this.parent = pParent;
      this.updateStatus = pUpdateStatus;
      this.newWorld = pNewWorld;
      this.worldLoadDuration = pWorldLoadDuration;
   }

   private void switchState(ClientHandshakePacketListenerImpl.State pState) {
      ClientHandshakePacketListenerImpl.State clienthandshakepacketlistenerimpl$state = this.state.updateAndGet((p_301527_) -> {
         if (!pState.fromStates.contains(p_301527_)) {
            throw new IllegalStateException("Tried to switch to " + pState + " from " + p_301527_ + ", but expected one of " + pState.fromStates);
         } else {
            return pState;
         }
      });
      this.updateStatus.accept(clienthandshakepacketlistenerimpl$state.message);
   }

   public void handleHello(ClientboundHelloPacket pPacket) {
      this.switchState(ClientHandshakePacketListenerImpl.State.AUTHORIZING);

      Cipher cipher;
      Cipher cipher1;
      String s;
      ServerboundKeyPacket serverboundkeypacket;
      try {
         SecretKey secretkey = Crypt.generateSecretKey();
         PublicKey publickey = pPacket.getPublicKey();
         s = (new BigInteger(Crypt.digestData(pPacket.getServerId(), publickey, secretkey))).toString(16);
         cipher = Crypt.getCipher(2, secretkey);
         cipher1 = Crypt.getCipher(1, secretkey);
         byte[] abyte = pPacket.getChallenge();
         serverboundkeypacket = new ServerboundKeyPacket(secretkey, publickey, abyte);
      } catch (Exception exception) {
         throw new IllegalStateException("Protocol error", exception);
      }

      Util.ioPool().submit(() -> {
         Component component = this.authenticateServer(s);
         if (component != null) {
            if (this.serverData == null || !this.serverData.isLan()) {
               this.connection.disconnect(component);
               return;
            }

            LOGGER.warn(component.getString());
         }

         this.switchState(ClientHandshakePacketListenerImpl.State.ENCRYPTING);
         this.connection.send(serverboundkeypacket, PacketSendListener.thenRun(() -> {
            this.connection.setEncryptionKey(cipher, cipher1);
         }));
      });
   }

   @Nullable
   private Component authenticateServer(String pServerHash) {
      final Connection mixinClientConnection =  connection;
      if (mixinClientConnection.viaFabricPlus$getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
         // We are in the 1.7 -> 1.6 protocol, so we need to skip the joinServer call
         // if the server is in offline mode, due the packet changes <-> networking changes
         // Minecraft's networking code is bad for us.
         if (!mixinClientConnection.viaFabricPlus$getUserConnection().get(ProtocolMetadataStorage.class).authenticate) {
            return null;
         }
      }
      try {
         this.getMinecraftSessionService().joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), pServerHash);
         return null;
      } catch (AuthenticationUnavailableException authenticationunavailableexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
      } catch (InvalidCredentialsException invalidcredentialsexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
      } catch (InsufficientPrivilegesException insufficientprivilegesexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
      } catch (ForcedUsernameChangeException | UserBannedException userbannedexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
      } catch (AuthenticationException authenticationexception) {
         return Component.translatable("disconnect.loginFailedInfo", authenticationexception.getMessage());
      }
   }

   private MinecraftSessionService getMinecraftSessionService() {
      return this.minecraft.getMinecraftSessionService();
   }

   public void handleGameProfile(ClientboundGameProfilePacket pPacket) {
      this.switchState(ClientHandshakePacketListenerImpl.State.JOINING);
      GameProfile gameprofile = pPacket.getGameProfile();
      this.connection.send(new ServerboundLoginAcknowledgedPacket());
      this.connection.setListener(new ClientConfigurationPacketListenerImpl(this.minecraft, this.connection, new CommonListenerCookie(gameprofile, this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName), ClientRegistryLayer.createRegistryAccess().compositeAccess(), FeatureFlags.DEFAULT_FLAGS, (String)null, this.serverData, this.parent)));
      this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
      this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
   }

   public void onDisconnect(Component pReason) {
      if (this.serverData != null && this.serverData.isRealm()) {
         this.minecraft.setScreen(new DisconnectedRealmsScreen(this.parent, CommonComponents.CONNECT_FAILED, pReason));
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, pReason));
      }

   }

   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public void handleDisconnect(ClientboundLoginDisconnectPacket pPacket) {
      this.connection.disconnect(pPacket.getReason());
   }

   public void handleCompression(ClientboundLoginCompressionPacket pPacket) {
      if (!this.connection.isMemoryConnection()) {
         this.connection.setupCompression(pPacket.getCompressionThreshold(), ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_17));
         ;
      }

   }

   public void handleCustomQuery(ClientboundCustomQueryPacket pPacket) {
      this.updateStatus.accept(Component.translatable("connect.negotiating"));
      this.connection.send(new ServerboundCustomQueryAnswerPacket(pPacket.transactionId(), (CustomQueryAnswerPayload)null));
   }

   public void setMinigameName(String pMinigameName) {
      this.minigameName = pMinigameName;
   }

   public void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
      pCrashReportCategory.setDetail("Server type", () -> {
         return this.serverData != null ? this.serverData.type().toString() : "<unknown>";
      });
      pCrashReportCategory.setDetail("Login phase", () -> {
         return this.state.get().toString();
      });
   }

   @OnlyIn(Dist.CLIENT)
   static enum State {
      CONNECTING(Component.translatable("connect.connecting"), Set.of()),
      AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
      ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
      JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

      final Component message;
      final Set<ClientHandshakePacketListenerImpl.State> fromStates;

      private State(Component pMessage, Set<ClientHandshakePacketListenerImpl.State> pFromStates) {
         this.message = pMessage;
         this.fromStates = pFromStates;
      }
   }
}