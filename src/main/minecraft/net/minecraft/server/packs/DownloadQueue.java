package net.minecraft.server.packs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class DownloadQueue implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_KEPT_PACKS = 20;
   private final Path cacheDir;
   private final JsonEventLog<DownloadQueue.LogEntry> eventLog;
   private final ProcessorMailbox<Runnable> tasks = ProcessorMailbox.create(Util.nonCriticalIoPool(), "download-queue");

   public DownloadQueue(Path pCacheDir) throws IOException {
      this.cacheDir = pCacheDir;
      FileUtil.createDirectoriesSafe(pCacheDir);
      this.eventLog = JsonEventLog.open(DownloadQueue.LogEntry.CODEC, pCacheDir.resolve("log.json"));
      DownloadCacheCleaner.vacuumCacheDir(pCacheDir, 20);
   }

   private DownloadQueue.BatchResult runDownload(DownloadQueue.BatchConfig pBatchConfig, Map<UUID, DownloadQueue.DownloadRequest> pDownloads) {
      DownloadQueue.BatchResult downloadqueue$batchresult = new DownloadQueue.BatchResult();
      pDownloads.forEach((p_311290_, p_311466_) -> {
         Path path = this.cacheDir.resolve(p_311290_.toString());
         Path path1 = null;

         try {
            path1 = HttpUtil.downloadFile(path, p_311466_.url, pBatchConfig.headers, pBatchConfig.hashFunction, p_311466_.hash, pBatchConfig.maxSize, pBatchConfig.proxy, pBatchConfig.listener);
            downloadqueue$batchresult.downloaded.put(p_311290_, path1);
         } catch (Exception exception1) {
            LOGGER.error("Failed to download {}", p_311466_.url, exception1);
            downloadqueue$batchresult.failed.add(p_311290_);
         }

         try {
            this.eventLog.write(new DownloadQueue.LogEntry(p_311290_, p_311466_.url.toString(), Instant.now(), Optional.ofNullable(p_311466_.hash).map(HashCode::toString), path1 != null ? this.getFileInfo(path1) : Either.left("download_failed")));
         } catch (Exception exception) {
            LOGGER.error("Failed to log download of {}", p_311466_.url, exception);
         }

      });
      return downloadqueue$batchresult;
   }

   private Either<String, DownloadQueue.FileInfoEntry> getFileInfo(Path pPath) {
      try {
         long i = Files.size(pPath);
         Path path = this.cacheDir.relativize(pPath);
         return Either.right(new DownloadQueue.FileInfoEntry(path.toString(), i));
      } catch (IOException ioexception) {
         LOGGER.error("Failed to get file size of {}", pPath, ioexception);
         return Either.left("no_access");
      }
   }

   public CompletableFuture<DownloadQueue.BatchResult> downloadBatch(DownloadQueue.BatchConfig pBatchConfig, Map<UUID, DownloadQueue.DownloadRequest> pDownloads) {
      return CompletableFuture.supplyAsync(() -> {
         return this.runDownload(pBatchConfig, pDownloads);
      }, this.tasks::tell);
   }

   public void close() throws IOException {
      this.tasks.close();
      this.eventLog.close();
   }

   public static record BatchConfig(HashFunction hashFunction, int maxSize, Map<String, String> headers, Proxy proxy, HttpUtil.DownloadProgressListener listener) {
   }

   public static record BatchResult(Map<UUID, Path> downloaded, Set<UUID> failed) {
      public BatchResult() {
         this(new HashMap<>(), new HashSet<>());
      }
   }

   public static record DownloadRequest(URL url, @Nullable HashCode hash) {
   }

   static record FileInfoEntry(String name, long size) {
      public static final Codec<DownloadQueue.FileInfoEntry> CODEC = RecordCodecBuilder.create((p_311514_) -> {
         return p_311514_.group(Codec.STRING.fieldOf("name").forGetter(DownloadQueue.FileInfoEntry::name), Codec.LONG.fieldOf("size").forGetter(DownloadQueue.FileInfoEntry::size)).apply(p_311514_, DownloadQueue.FileInfoEntry::new);
      });
   }

   static record LogEntry(UUID id, String url, Instant time, Optional<String> hash, Either<String, DownloadQueue.FileInfoEntry> errorOrFileInfo) {
      public static final Codec<DownloadQueue.LogEntry> CODEC = RecordCodecBuilder.create((p_310865_) -> {
         return p_310865_.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DownloadQueue.LogEntry::id), Codec.STRING.fieldOf("url").forGetter(DownloadQueue.LogEntry::url), ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(DownloadQueue.LogEntry::time), Codec.STRING.optionalFieldOf("hash").forGetter(DownloadQueue.LogEntry::hash), Codec.mapEither(Codec.STRING.fieldOf("error"), DownloadQueue.FileInfoEntry.CODEC.fieldOf("file")).forGetter(DownloadQueue.LogEntry::errorOrFileInfo)).apply(p_310865_, DownloadQueue.LogEntry::new);
      });
   }
}