package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Minecraft minecraft;
   protected final Connection connection;
   @Nullable
   protected final ServerData serverData;
   @Nullable
   protected String serverBrand;
   protected final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   protected final Screen postDisconnectScreen;
   private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();

   protected ClientCommonPacketListenerImpl(Minecraft pMinecraft, Connection pConnection, CommonListenerCookie pCommonListenerCookie) {
      this.minecraft = pMinecraft;
      this.connection = pConnection;
      this.serverData = pCommonListenerCookie.serverData();
      this.serverBrand = pCommonListenerCookie.serverBrand();
      this.telemetryManager = pCommonListenerCookie.telemetryManager();
      this.postDisconnectScreen = pCommonListenerCookie.postDisconnectScreen();
   }

   public void handleKeepAlive(ClientboundKeepAlivePacket pPacket) {
      this.sendWhen(new ServerboundKeepAlivePacket(pPacket.getId()), () -> {
         return !RenderSystem.isFrozenAtPollEvents();
      }, Duration.ofMinutes(1L));
   }

   public void handlePing(ClientboundPingPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      this.send(new ServerboundPongPacket(pPacket.getId()));
   }

   public void handleCustomPayload(ClientboundCustomPayloadPacket pPacket) {
      CustomPacketPayload custompacketpayload = pPacket.payload();
      if (!(custompacketpayload instanceof DiscardedPayload)) {
         PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
         if (custompacketpayload instanceof BrandPayload) {
            BrandPayload brandpayload = (BrandPayload)custompacketpayload;
            this.serverBrand = brandpayload.brand();
            this.telemetryManager.onServerBrandReceived(brandpayload.brand());
         } else {
            this.handleCustomPayload(custompacketpayload);
         }

      }
   }

   protected abstract void handleCustomPayload(CustomPacketPayload pPayload);

   protected abstract RegistryAccess.Frozen registryAccess();

   public void handleResourcePackPush(ClientboundResourcePackPushPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      UUID uuid = pPacket.id();
      URL url = parseResourcePackUrl(pPacket.url());
      if (url == null) {
         this.connection.send(new ServerboundResourcePackPacket(uuid, ServerboundResourcePackPacket.Action.INVALID_URL));
      } else {
         String s = pPacket.hash();
         boolean flag = pPacket.required();
         ServerData.ServerPackStatus serverdata$serverpackstatus = this.serverData != null ? this.serverData.getResourcePackStatus() : ServerData.ServerPackStatus.PROMPT;
         if (serverdata$serverpackstatus != ServerData.ServerPackStatus.PROMPT && (!flag || serverdata$serverpackstatus != ServerData.ServerPackStatus.DISABLED)) {
            this.minecraft.getDownloadedPackSource().pushPack(uuid, url, s);
         } else {
            this.minecraft.setScreen(this.addOrUpdatePackPrompt(uuid, url, s, flag, pPacket.prompt()));
         }

      }
   }

   public void handleResourcePackPop(ClientboundResourcePackPopPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      pPacket.id().ifPresentOrElse((p_308277_) -> {
         this.minecraft.getDownloadedPackSource().popPack(p_308277_);
      }, () -> {
         this.minecraft.getDownloadedPackSource().popAll();
      });
   }

   static Component preparePackPrompt(Component pLine1, @Nullable Component pLine2) {
      return (Component)(pLine2 == null ? pLine1 : Component.translatable("multiplayer.texturePrompt.serverPrompt", pLine1, pLine2));
   }

   @Nullable
   private static URL parseResourcePackUrl(String pUrl) {
      try {
         URL url = new URL(pUrl);
         String s = url.getProtocol();
         return !"http".equals(s) && !"https".equals(s) ? null : url;
      } catch (MalformedURLException malformedurlexception) {
         return null;
      }
   }

   public void handleUpdateTags(ClientboundUpdateTagsPacket pPacket) {
      PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
      pPacket.getTags().forEach(this::updateTagsForRegistry);
   }

   private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> p_301094_, TagNetworkSerialization.NetworkPayload p_297701_) {
      if (!p_297701_.isEmpty()) {
         Registry<T> registry = this.registryAccess().registry(p_301094_).orElseThrow(() -> {
            return new IllegalStateException("Unknown registry " + p_301094_);
         });
         Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
         TagNetworkSerialization.deserializeTagsFromNetwork((ResourceKey<? extends Registry<T>>)p_301094_, registry, p_297701_, map::put);
         registry.bindTags(map);
      }
   }

   public void handleDisconnect(ClientboundDisconnectPacket pPacket) {
      this.connection.disconnect(pPacket.getReason());
   }

   protected void sendDeferredPackets() {
      Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

      while(iterator.hasNext()) {
         ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();
         if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean()) {
            this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
            iterator.remove();
         } else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis()) {
            iterator.remove();
         }
      }

   }

   public void send(Packet<?> pPacket) {
      this.connection.send(pPacket);
   }

   public void onDisconnect(Component pReason) {
      this.telemetryManager.onDisconnect();
      this.minecraft.disconnect(this.createDisconnectScreen(pReason));
      LOGGER.warn("Client disconnected with reason: {}", (Object)pReason.getString());
   }

   public void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
      pCrashReportCategory.setDetail("Server type", () -> {
         return this.serverData != null ? this.serverData.type().toString() : "<none>";
      });
      pCrashReportCategory.setDetail("Server brand", () -> {
         return this.serverBrand;
      });
   }

   protected Screen createDisconnectScreen(Component pReason) {
      Screen screen = Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> {
         return new JoinMultiplayerScreen(new TitleScreen());
      });
      return (Screen)(this.serverData != null && this.serverData.isRealm() ? new DisconnectedRealmsScreen(screen, GENERIC_DISCONNECT_MESSAGE, pReason) : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, pReason));
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   private void sendWhen(Packet<? extends ServerboundPacketListener> pPacket, BooleanSupplier pSendCondition, Duration pExpirationTime) {
      if (pSendCondition.getAsBoolean()) {
         this.send(pPacket);
      } else {
         this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(pPacket, pSendCondition, Util.getMillis() + pExpirationTime.toMillis()));
      }

   }

   private Screen addOrUpdatePackPrompt(UUID pId, URL pUrl, String pHash, boolean pRequired, @Nullable Component pPrompt) {
      Screen screen = this.minecraft.screen;
      if (screen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen clientcommonpacketlistenerimpl$packconfirmscreen) {
         return clientcommonpacketlistenerimpl$packconfirmscreen.update(this.minecraft, pId, pUrl, pHash, pRequired, pPrompt);
      } else {
         return new ClientCommonPacketListenerImpl.PackConfirmScreen(this.minecraft, screen, List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(pId, pUrl, pHash)), pRequired, pPrompt);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }

   @OnlyIn(Dist.CLIENT)
   class PackConfirmScreen extends ConfirmScreen {
      private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
      @Nullable
      private final Screen parentScreen;

      PackConfirmScreen(Minecraft pMinecraft, @Nullable Screen pParentScreen, List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> pRequests, boolean pRequired, @Nullable Component pPrompt) {
         super((p_309396_) -> {
            pMinecraft.setScreen(pParentScreen);
            DownloadedPackSource downloadedpacksource = pMinecraft.getDownloadedPackSource();
            if (p_309396_) {
               if (ClientCommonPacketListenerImpl.this.serverData != null) {
                  ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
               }

               downloadedpacksource.allowServerPacks();
            } else {
               downloadedpacksource.rejectServerPacks();
               if (pRequired) {
                  ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
               } else if (ClientCommonPacketListenerImpl.this.serverData != null) {
                  ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
               }
            }

            for(ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest : pRequests) {
               downloadedpacksource.pushPack(clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.id, clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.url, clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.hash);
            }

            if (ClientCommonPacketListenerImpl.this.serverData != null) {
               ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
            }

         }, pRequired ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"), ClientCommonPacketListenerImpl.preparePackPrompt(pRequired ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD) : Component.translatable("multiplayer.texturePrompt.line2"), pPrompt), pRequired ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, pRequired ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO);
         this.requests = pRequests;
         this.parentScreen = pParentScreen;
      }

      public ClientCommonPacketListenerImpl.PackConfirmScreen update(Minecraft pMinecraft, UUID pId, URL pUrl, String pHash, boolean pRequired, @Nullable Component pPrompt) {
         List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list = ImmutableList.<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest>builderWithExpectedSize(this.requests.size() + 1).addAll(this.requests).add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(pId, pUrl, pHash)).build();
         return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(pMinecraft, this.parentScreen, list, pRequired, pPrompt);
      }

      @OnlyIn(Dist.CLIENT)
      static record PendingRequest(UUID id, URL url, String hash) {
      }
   }
}