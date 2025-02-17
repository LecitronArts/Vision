package net.minecraft.client.resources.server;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.util.UndashedUuid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.data.ResourcePackHeaderDiff;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.main.GameConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.packs.DownloadQueue;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DownloadedPackSource implements AutoCloseable {
   private static final Component SERVER_NAME = Component.translatable("resourcePack.server.name");
   private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final RepositorySource EMPTY_SOURCE = (p_313076_) -> {
   };
   private static final PackLoadFeedback LOG_ONLY_FEEDBACK = new PackLoadFeedback() {
      public void reportUpdate(UUID p_310776_, PackLoadFeedback.Update p_309862_) {
         DownloadedPackSource.LOGGER.debug("Downloaded pack {} changed state to {}", p_310776_, p_309862_);
      }

      public void reportFinalResult(UUID p_310730_, PackLoadFeedback.FinalResult p_311165_) {
         DownloadedPackSource.LOGGER.debug("Downloaded pack {} finished with state {}", p_310730_, p_311165_);
      }
   };
   final Minecraft minecraft;
   private RepositorySource packSource = EMPTY_SOURCE;
   @Nullable
   private PackReloadConfig.Callbacks pendingReload;
   final ServerPackManager manager;
   private final DownloadQueue downloadQueue;
   private PackSource packType = PackSource.SERVER;
   PackLoadFeedback packFeedback = LOG_ONLY_FEEDBACK;
   private int packIdSerialNumber;

   public DownloadedPackSource(Minecraft pMinecraft, Path pDirectory, GameConfig.UserData pUserData) {
      this.minecraft = pMinecraft;

      try {
         this.downloadQueue = new DownloadQueue(pDirectory);
      } catch (IOException ioexception) {
         throw new UncheckedIOException("Failed to open download queue in directory " + pDirectory, ioexception);
      }

      Executor executor = pMinecraft::tell;
      this.manager = new ServerPackManager(this.createDownloader(this.downloadQueue, executor, pUserData.user, pUserData.proxy), new PackLoadFeedback() {
         public void reportUpdate(UUID p_311063_, PackLoadFeedback.Update p_310840_) {
            DownloadedPackSource.this.packFeedback.reportUpdate(p_311063_, p_310840_);
         }

         public void reportFinalResult(UUID p_311502_, PackLoadFeedback.FinalResult p_310552_) {
            DownloadedPackSource.this.packFeedback.reportFinalResult(p_311502_, p_310552_);
         }
      }, this.createReloadConfig(), this.createUpdateScheduler(executor), ServerPackManager.PackPromptStatus.PENDING);
   }

   HttpUtil.DownloadProgressListener createDownloadNotifier(final int pPackCount) {
      return new HttpUtil.DownloadProgressListener() {
         private final SystemToast.SystemToastId toastId = new SystemToast.SystemToastId();
         private Component title = Component.empty();
         @Nullable
         private Component message = null;
         private int count;
         private int failCount;
         private OptionalLong totalBytes = OptionalLong.empty();

         private void updateToast() {
            SystemToast.addOrUpdate(DownloadedPackSource.this.minecraft.getToasts(), this.toastId, this.title, this.message);
         }

         private void updateProgress(long p_310910_) {
            if (this.totalBytes.isPresent()) {
               this.message = Component.translatable("download.pack.progress.percent", p_310910_ * 100L / this.totalBytes.getAsLong());
            } else {
               this.message = Component.translatable("download.pack.progress.bytes", Unit.humanReadable(p_310910_));
            }

            this.updateToast();
         }

         public void requestStart() {
            ++this.count;
            this.title = Component.translatable("download.pack.title", this.count, pPackCount);
            this.updateToast();
            DownloadedPackSource.LOGGER.debug("Starting pack {}/{} download", this.count, pPackCount);
         }

         public void downloadStart(OptionalLong p_309831_) {
            DownloadedPackSource.LOGGER.debug("File size = {} bytes", (Object)p_309831_);
            this.totalBytes = p_309831_;
            this.updateProgress(0L);
         }

         public void downloadedBytes(long p_313004_) {
            DownloadedPackSource.LOGGER.debug("Progress for pack {}: {} bytes", this.count, p_313004_);
            this.updateProgress(p_313004_);
         }

         public void requestFinished(boolean p_311561_) {
            if (!p_311561_) {
               DownloadedPackSource.LOGGER.info("Pack {} failed to download", (int)this.count);
               ++this.failCount;
            } else {
               DownloadedPackSource.LOGGER.debug("Download ended for pack {}", (int)this.count);
            }

            if (this.count == pPackCount) {
               if (this.failCount > 0) {
                  this.title = Component.translatable("download.pack.failed", this.failCount, pPackCount);
                  this.message = null;
                  this.updateToast();
               } else {
                  SystemToast.forceHide(DownloadedPackSource.this.minecraft.getToasts(), this.toastId);
               }
            }

         }
      };
   }

   private PackDownloader createDownloader(final DownloadQueue pDownloadQueue, final Executor pExecutor, final User pUser, final Proxy pProxy) {
      return new PackDownloader() {
         private static final int MAX_PACK_SIZE_BYTES = 262144000;
         private static final HashFunction CACHE_HASHING_FUNCTION = Hashing.sha1();

         private Map<String, String> createDownloadHeaders() {
            WorldVersion worldversion = ResourcePackHeaderDiff.get(ProtocolTranslator.getTargetVersion());//SharedConstants.getCurrentVersion();

            final LinkedHashMap<String, String> modifiableMap = new LinkedHashMap<>(Map.of("X-Minecraft-Username", pUser.getName(), "X-Minecraft-UUID", UndashedUuid.toString(pUser.getProfileId()), "X-Minecraft-Version", worldversion.getName(), "X-Minecraft-Version-ID", worldversion.getId(), "X-Minecraft-Pack-Format", String.valueOf(worldversion.getPackVersion(PackType.CLIENT_RESOURCES)), "User-Agent", "Minecraft Java/" + worldversion.getName()));
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
               modifiableMap.remove("X-Minecraft-Version-ID");
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
                  modifiableMap.remove("X-Minecraft-Pack-Format");
                  modifiableMap.remove("User-Agent");
               }
            }
            return (modifiableMap);
/*            return Map.of("X-Minecraft-Username", pUser.getName(), "X-Minecraft-UUID", UndashedUuid.toString(pUser.getProfileId()), "X-Minecraft-Version", worldversion.getName(), "X-Minecraft-Version-ID", worldversion.getId(), "X-Minecraft-Pack-Format", String.valueOf(worldversion.getPackVersion(PackType.CLIENT_RESOURCES)), "User-Agent", "Minecraft Java/" + worldversion.getName());
        */ }

         public void download(Map<UUID, DownloadQueue.DownloadRequest> p_310177_, Consumer<DownloadQueue.BatchResult> p_310806_) {
            pDownloadQueue.downloadBatch(new DownloadQueue.BatchConfig(CACHE_HASHING_FUNCTION, 262144000, this.createDownloadHeaders(), pProxy, DownloadedPackSource.this.createDownloadNotifier(p_310177_.size())), p_310177_).thenAcceptAsync(p_310806_, pExecutor);
         }
      };
   }

   private Runnable createUpdateScheduler(final Executor pExecutor) {
      return new Runnable() {
         private boolean scheduledInMainExecutor;
         private boolean hasUpdates;

         public void run() {
            this.hasUpdates = true;
            if (!this.scheduledInMainExecutor) {
               this.scheduledInMainExecutor = true;
               pExecutor.execute(this::runAllUpdates);
            }

         }

         private void runAllUpdates() {
            while(this.hasUpdates) {
               this.hasUpdates = false;
               DownloadedPackSource.this.manager.tick();
            }

            this.scheduledInMainExecutor = false;
         }
      };
   }

   private PackReloadConfig createReloadConfig() {
      return this::startReload;
   }

   @Nullable
   private List<Pack> loadRequestedPacks(List<PackReloadConfig.IdAndPath> pPacks) {
      List<Pack> list = new ArrayList<>(pPacks.size());

      for(PackReloadConfig.IdAndPath packreloadconfig$idandpath : Lists.reverse(pPacks)) {
         String s = String.format(Locale.ROOT, "server/%08X/%s", this.packIdSerialNumber++, packreloadconfig$idandpath.id());
         Path path = packreloadconfig$idandpath.path();
         Pack.ResourcesSupplier pack$resourcessupplier = new FilePackResources.FileResourcesSupplier(path, false);
         int i = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
         Pack.Info pack$info = Pack.readPackInfo(s, pack$resourcessupplier, i);
         if (pack$info == null) {
            LOGGER.warn("Invalid pack metadata in {}, ignoring all", (Object)path);
            return null;
         }

         list.add(Pack.create(s, SERVER_NAME, true, pack$resourcessupplier, pack$info, Pack.Position.TOP, true, this.packType));
      }

      return list;
   }

   public RepositorySource createRepositorySource() {
      return (p_311800_) -> {
         this.packSource.loadPacks(p_311800_);
      };
   }

   private static RepositorySource configureSource(List<Pack> pPacks) {
      return pPacks.isEmpty() ? EMPTY_SOURCE : pPacks::forEach;
   }

   private void startReload(PackReloadConfig.Callbacks p_310818_) {
      this.pendingReload = p_310818_;
      List<PackReloadConfig.IdAndPath> list = p_310818_.packsToLoad();
      List<Pack> list1 = this.loadRequestedPacks(list);
      if (list1 == null) {
         p_310818_.onFailure(false);
         List<PackReloadConfig.IdAndPath> list2 = p_310818_.packsToLoad();
         list1 = this.loadRequestedPacks(list2);
         if (list1 == null) {
            LOGGER.warn("Double failure in loading server packs");
            list1 = List.of();
         }
      }

      this.packSource = configureSource(list1);
      this.minecraft.reloadResourcePacks();
   }

   public void onRecovery() {
      if (this.pendingReload != null) {
         this.pendingReload.onFailure(false);
         List<Pack> list = this.loadRequestedPacks(this.pendingReload.packsToLoad());
         if (list == null) {
            LOGGER.warn("Double failure in loading server packs");
            list = List.of();
         }

         this.packSource = configureSource(list);
      }

   }

   public void onRecoveryFailure() {
      if (this.pendingReload != null) {
         this.pendingReload.onFailure(true);
         this.pendingReload = null;
         this.packSource = EMPTY_SOURCE;
      }

   }

   public void onReloadSuccess() {
      if (this.pendingReload != null) {
         this.pendingReload.onSuccess();
         this.pendingReload = null;
      }

   }

   @Nullable
   private static HashCode tryParseSha1Hash(@Nullable String pHash) {
      return pHash != null && SHA1.matcher(pHash).matches() ? HashCode.fromString(pHash.toLowerCase(Locale.ROOT)) : null;
   }

   public void pushPack(UUID pUuid, URL pUrl, @Nullable String pHash) {
      HashCode hashcode = tryParseSha1Hash(pHash);
      this.manager.pushPack(pUuid, pUrl, hashcode);
   }

   public void pushLocalPack(UUID pUuid, Path pPath) {
      this.manager.pushLocalPack(pUuid, pPath);
   }

   public void popPack(UUID pUuid) {
      this.manager.popPack(pUuid);
   }

   public void popAll() {
      this.manager.popAll();
   }

   private static PackLoadFeedback createPackResponseSender(final Connection pConnection) {
      return new PackLoadFeedback() {
         public void reportUpdate(UUID p_310120_, PackLoadFeedback.Update p_313074_) {
            DownloadedPackSource.LOGGER.debug("Pack {} changed status to {}", p_310120_, p_313074_);
            ServerboundResourcePackPacket.Action serverboundresourcepackpacket$action1;
            switch (p_313074_) {
               case ACCEPTED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.ACCEPTED;
                  break;
               case DOWNLOADED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.DOWNLOADED;
                  break;
               default:
                  throw new IncompatibleClassChangeError();
            }

            ServerboundResourcePackPacket.Action serverboundresourcepackpacket$action = serverboundresourcepackpacket$action1;
            pConnection.send(new ServerboundResourcePackPacket(p_310120_, serverboundresourcepackpacket$action));
         }

         public void reportFinalResult(UUID p_310323_, PackLoadFeedback.FinalResult p_312396_) {
            DownloadedPackSource.LOGGER.debug("Pack {} changed status to {}", p_310323_, p_312396_);
            ServerboundResourcePackPacket.Action serverboundresourcepackpacket$action1;
            switch (p_312396_) {
               case APPLIED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED;
                  break;
               case DOWNLOAD_FAILED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD;
                  break;
               case DECLINED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.DECLINED;
                  break;
               case DISCARDED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.DISCARDED;
                  break;
               case ACTIVATION_FAILED:
                  serverboundresourcepackpacket$action1 = ServerboundResourcePackPacket.Action.FAILED_RELOAD;
                  break;
               default:
                  throw new IncompatibleClassChangeError();
            }

            ServerboundResourcePackPacket.Action serverboundresourcepackpacket$action = serverboundresourcepackpacket$action1;
            pConnection.send(new ServerboundResourcePackPacket(p_310323_, serverboundresourcepackpacket$action));
         }
      };
   }

   public void configureForServerControl(Connection pConnection, ServerPackManager.PackPromptStatus pPackPromptStatus) {
      this.packType = PackSource.SERVER;
      this.packFeedback = createPackResponseSender(pConnection);
      switch (pPackPromptStatus) {
         case ALLOWED:
            this.manager.allowServerPacks();
            break;
         case DECLINED:
            this.manager.rejectServerPacks();
            break;
         case PENDING:
            this.manager.resetPromptStatus();
      }

   }

   public void configureForLocalWorld() {
      this.packType = PackSource.WORLD;
      this.packFeedback = LOG_ONLY_FEEDBACK;
      this.manager.allowServerPacks();
   }

   public void allowServerPacks() {
      this.manager.allowServerPacks();
   }

   public void rejectServerPacks() {
      this.manager.rejectServerPacks();
   }

   public CompletableFuture<Void> waitForPackFeedback(final UUID pUuid) {
      final CompletableFuture<Void> completablefuture = new CompletableFuture<>();
      final PackLoadFeedback packloadfeedback = this.packFeedback;
      this.packFeedback = new PackLoadFeedback() {
         public void reportUpdate(UUID p_312518_, PackLoadFeedback.Update p_310008_) {
            packloadfeedback.reportUpdate(p_312518_, p_310008_);
         }

         public void reportFinalResult(UUID p_310518_, PackLoadFeedback.FinalResult p_310501_) {
            if (pUuid.equals(p_310518_)) {
               DownloadedPackSource.this.packFeedback = packloadfeedback;
               if (p_310501_ == PackLoadFeedback.FinalResult.APPLIED) {
                  completablefuture.complete((Void)null);
               } else {
                  completablefuture.completeExceptionally(new IllegalStateException("Failed to apply pack " + p_310518_ + ", reason: " + p_310501_));
               }
            }

            packloadfeedback.reportFinalResult(p_310518_, p_310501_);
         }
      };
      return completablefuture;
   }

   public void cleanupAfterDisconnect() {
      this.manager.popAll();
      this.packFeedback = LOG_ONLY_FEEDBACK;
      this.manager.resetPromptStatus();
   }

   public void close() throws IOException {
      this.downloadQueue.close();
   }
}