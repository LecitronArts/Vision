package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import net.optifine.reflect.Reflector;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
   private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
   public static final int MIN_VIEW_DISTANCE = 2;
   public static final int MAX_VIEW_DISTANCE = 32;
   public static final int FORCED_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap<>();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap<>();
   private final LongSet entitiesInLevel = new LongOpenHashSet();
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private ChunkGenerator generator;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final Supplier<DimensionDataStorage> overworldDataStorage;
   private final PoiManager poiManager;
   final LongSet toDrop = new LongOpenHashSet();
   private boolean modified;
   private final ChunkTaskPriorityQueueSorter queueSorter;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
   private final ChunkProgressListener progressListener;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final AtomicInteger tickingGenerated = new AtomicInteger();
   private final StructureTemplateManager structureTemplateManager;
   private final String storageName;
   private final PlayerMap playerMap = new PlayerMap();
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap<>();
   private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
   private final Long2LongMap chunkSaveCooldowns = new Long2LongOpenHashMap();
   private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
   private int serverViewDistance;

   public ChunkMap(ServerLevel pLevel, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, DataFixer pFixerUpper, StructureTemplateManager pStructureManager, Executor pDispatcher, BlockableEventLoop<Runnable> pMainThreadExecutor, LightChunkGetter pLightChunk, ChunkGenerator pGenerator, ChunkProgressListener pProgressListener, ChunkStatusUpdateListener pChunkStatusListener, Supplier<DimensionDataStorage> pOverworldDataStorage, int pViewDistance, boolean pSync) {
      super(pLevelStorageAccess.getDimensionPath(pLevel.dimension()).resolve("region"), pFixerUpper, pSync);
      this.structureTemplateManager = pStructureManager;
      Path path = pLevelStorageAccess.getDimensionPath(pLevel.dimension());
      this.storageName = path.getFileName().toString();
      this.level = pLevel;
      this.generator = pGenerator;
      RegistryAccess registryaccess = pLevel.registryAccess();
      long i = pLevel.getSeed();
      if (pGenerator instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator) {
         this.randomState = RandomState.create(noisebasedchunkgenerator.generatorSettings().value(), registryaccess.lookupOrThrow(Registries.NOISE), i);
      } else {
         this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryaccess.lookupOrThrow(Registries.NOISE), i);
      }

      this.chunkGeneratorState = pGenerator.createState(registryaccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, i);
      this.mainThreadExecutor = pMainThreadExecutor;
      ProcessorMailbox<Runnable> processormailbox1 = ProcessorMailbox.create(pDispatcher, "worldgen");
      ProcessorHandle<Runnable> processorhandle = ProcessorHandle.of("main", pMainThreadExecutor::tell);
      this.progressListener = pProgressListener;
      this.chunkStatusListener = pChunkStatusListener;
      ProcessorMailbox<Runnable> processormailbox = ProcessorMailbox.create(pDispatcher, "light");
      this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(processormailbox1, processorhandle, processormailbox), pDispatcher, Integer.MAX_VALUE);
      this.worldgenMailbox = this.queueSorter.getProcessor(processormailbox1, false);
      this.mainThreadMailbox = this.queueSorter.getProcessor(processorhandle, false);
      this.lightEngine = new ThreadedLevelLightEngine(pLightChunk, this, this.level.dimensionType().hasSkyLight(), processormailbox, this.queueSorter.getProcessor(processormailbox, false));
      this.distanceManager = new ChunkMap.DistanceManager(pDispatcher, pMainThreadExecutor);
      this.overworldDataStorage = pOverworldDataStorage;
      this.poiManager = new PoiManager(path.resolve("poi"), pFixerUpper, pSync, registryaccess, pLevel);
      this.setServerViewDistance(pViewDistance);
   }

   protected ChunkGenerator generator() {
      return this.generator;
   }

   protected ChunkGeneratorStructureState generatorState() {
      return this.chunkGeneratorState;
   }

   protected RandomState randomState() {
      return this.randomState;
   }

   public void debugReloadGenerator() {
      DataResult<JsonElement> dataresult = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
      DataResult<ChunkGenerator> dataresult1 = dataresult.flatMap((p_183803_0_) -> {
         return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, p_183803_0_);
      });
      dataresult1.result().ifPresent((p_183807_1_) -> {
         this.generator = p_183807_1_;
      });
   }

   private static double euclideanDistanceSquared(ChunkPos pChunkPos, Entity pEntity) {
      double d0 = (double)SectionPos.sectionToBlockCoord(pChunkPos.x, 8);
      double d1 = (double)SectionPos.sectionToBlockCoord(pChunkPos.z, 8);
      double d2 = d0 - pEntity.getX();
      double d3 = d1 - pEntity.getZ();
      return d2 * d2 + d3 * d3;
   }

   boolean isChunkTracked(ServerPlayer pPlayer, int pX, int pZ) {
      return pPlayer.getChunkTrackingView().contains(pX, pZ) && !pPlayer.connection.chunkSender.isPending(ChunkPos.asLong(pX, pZ));
   }

   private boolean isChunkOnTrackedBorder(ServerPlayer pPlayer, int pX, int pZ) {
      if (!this.isChunkTracked(pPlayer, pX, pZ)) {
         return false;
      } else {
         for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
               if ((i != 0 || j != 0) && !this.isChunkTracked(pPlayer, pX + i, pZ + j)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   protected ChunkHolder getUpdatingChunkIfPresent(long pChunkPos) {
      return this.updatingChunkMap.get(pChunkPos);
   }

   @Nullable
   protected ChunkHolder getVisibleChunkIfPresent(long pChunkPos) {
      return this.visibleChunkMap.get(pChunkPos);
   }

   protected IntSupplier getChunkQueueLevel(long pChunkPos) {
      return () -> {
         ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
         return chunkholder == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(chunkholder.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos pPos) {
      ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pPos.toLong());
      if (chunkholder == null) {
         return "null";
      } else {
         String s = chunkholder.getTicketLevel() + "\n";
         ChunkStatus chunkstatus = chunkholder.getLastAvailableStatus();
         ChunkAccess chunkaccess = chunkholder.getLastAvailable();
         if (chunkstatus != null) {
            s = s + "St: \u00a7" + chunkstatus.getIndex() + chunkstatus + "\u00a7r\n";
         }

         if (chunkaccess != null) {
            s = s + "Ch: \u00a7" + chunkaccess.getStatus().getIndex() + chunkaccess.getStatus() + "\u00a7r\n";
         }

         FullChunkStatus fullchunkstatus = chunkholder.getFullStatus();
         s = s + String.valueOf('\u00a7') + fullchunkstatus.ordinal() + fullchunkstatus;
         return s + "\u00a7r";
      }
   }

   private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkHolder pChunkHolder, int pRange, IntFunction<ChunkStatus> pStatusGetter) {
      if (pRange == 0) {
         ChunkStatus chunkstatus1 = pStatusGetter.apply(0);
         return pChunkHolder.getOrScheduleFuture(chunkstatus1, this).thenApply((p_214892_0_) -> {
            return p_214892_0_.mapLeft(List::of);
         });
      } else {
         List<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> list = new ArrayList<>();
         List<ChunkHolder> list1 = new ArrayList<>();
         ChunkPos chunkpos = pChunkHolder.getPos();
         int i = chunkpos.x;
         int j = chunkpos.z;

         for(int k = -pRange; k <= pRange; ++k) {
            for(int l = -pRange; l <= pRange; ++l) {
               int i1 = Math.max(Math.abs(l), Math.abs(k));
               final ChunkPos chunkpos1 = new ChunkPos(i + l, j + k);
               long j1 = chunkpos1.toLong();
               ChunkHolder chunkholder = this.getUpdatingChunkIfPresent(j1);
               if (chunkholder == null) {
                  return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                     public String toString() {
                        return "Unloaded " + chunkpos1;
                     }
                  }));
               }

               ChunkStatus chunkstatus = pStatusGetter.apply(i1);
               CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = chunkholder.getOrScheduleFuture(chunkstatus, this);
               list1.add(chunkholder);
               list.add(completablefuture);
            }
         }

         CompletableFuture<List<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completablefuture1 = Util.sequence(list);
         CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture2 = completablefuture1.thenApply((p_183726_4_) -> {
            List<ChunkAccess> list2 = Lists.newArrayList();
            int k1 = 0;

            for(final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either : p_183726_4_) {
               if (either == null) {
                  throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
               }

               Optional<ChunkAccess> optional = either.left();
               if (optional.isEmpty()) {
                  final int l1 = k1;
                  return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                     public String toString() {
                        return "Unloaded " + new ChunkPos(i + l1 % (pRange * 2 + 1), j + l1 / (pRange * 2 + 1)) + " " + either.right().get();
                     }
                  });
               }

               list2.add(optional.get());
               ++k1;
            }

            return Either.left(list2);
         });

         for(ChunkHolder chunkholder1 : list1) {
            chunkholder1.addSaveDependency("getChunkRangeFuture " + chunkpos + " " + pRange, completablefuture2);
         }

         return completablefuture2;
      }
   }

   public ReportedException debugFuturesAndCreateReportedException(IllegalStateException pException, String pDetails) {
      StringBuilder stringbuilder = new StringBuilder();
      Consumer<ChunkHolder> consumer = (p_203754_1_) -> {
         p_203754_1_.getAllFutures().forEach((p_203757_2_) -> {
            ChunkStatus chunkstatus = p_203757_2_.getFirst();
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = p_203757_2_.getSecond();
            if (completablefuture != null && completablefuture.isDone() && completablefuture.join() == null) {
               stringbuilder.append((Object)p_203754_1_.getPos()).append(" - status: ").append((Object)chunkstatus).append(" future: ").append((Object)completablefuture).append(System.lineSeparator());
            }

         });
      };
      stringbuilder.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(consumer);
      stringbuilder.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(consumer);
      CrashReport crashreport = CrashReport.forThrowable(pException, "Chunk loading");
      CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk loading");
      crashreportcategory.setDetail("Details", pDetails);
      crashreportcategory.setDetail("Futures", stringbuilder);
      return new ReportedException(crashreport);
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkHolder pChunk) {
      return this.getChunkRangeFuture(pChunk, 2, (p_203077_0_) -> {
         return ChunkStatus.FULL;
      }).thenApplyAsync((p_212877_0_) -> {
         return p_212877_0_.mapLeft((p_214938_0_) -> {
            return (LevelChunk)p_214938_0_.get(p_214938_0_.size() / 2);
         });
      }, this.mainThreadExecutor);
   }

   @Nullable
   ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel) {
      if (!ChunkLevel.isLoaded(pOldLevel) && !ChunkLevel.isLoaded(pNewLevel)) {
         return pHolder;
      } else {
         if (pHolder != null) {
            pHolder.setTicketLevel(pNewLevel);
         }

         if (pHolder != null) {
            if (!ChunkLevel.isLoaded(pNewLevel)) {
               this.toDrop.add(pChunkPos);
            } else {
               this.toDrop.remove(pChunkPos);
            }
         }

         if (ChunkLevel.isLoaded(pNewLevel) && pHolder == null) {
            pHolder = this.pendingUnloads.remove(pChunkPos);
            if (pHolder != null) {
               pHolder.setTicketLevel(pNewLevel);
            } else {
               pHolder = new ChunkHolder(new ChunkPos(pChunkPos), pNewLevel, this.level, this.lightEngine, this.queueSorter, this);
            }

            this.updatingChunkMap.put(pChunkPos, pHolder);
            this.modified = true;
         }

         if (Reflector.ForgeEventFactory_fireChunkTicketLevelUpdated.exists()) {
            Reflector.ForgeEventFactory_fireChunkTicketLevelUpdated.call(this.level, pChunkPos, pOldLevel, pNewLevel, pHolder);
         }

         return pHolder;
      }
   }

   public void close() throws IOException {
      try {
         this.queueSorter.close();
         this.poiManager.close();
      } finally {
         super.close();
      }

   }

   protected void saveAllChunks(boolean pFlush) {
      if (pFlush) {
         List<ChunkHolder> list = this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).toList();
         MutableBoolean mutableboolean = new MutableBoolean();

         do {
            mutableboolean.setFalse();
            list.stream().map((p_203101_1_) -> {
               CompletableFuture<ChunkAccess> completablefuture;
               do {
                  completablefuture = p_203101_1_.getChunkToSave();
                  this.mainThreadExecutor.managedBlock(completablefuture::isDone);
               } while(completablefuture != p_203101_1_.getChunkToSave());

               return completablefuture.join();
            }).filter((p_203087_0_) -> {
               return p_203087_0_ instanceof ImposterProtoChunk || p_203087_0_ instanceof LevelChunk;
            }).filter(this::save).forEach((p_203049_1_) -> {
               mutableboolean.setTrue();
            });
         } while(mutableboolean.isTrue());

         this.processUnloads(() -> {
            return true;
         });
         this.flushWorker();
      } else {
         this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
      }

   }

   protected void tick(BooleanSupplier pHasMoreTime) {
      ProfilerFiller profilerfiller = this.level.getProfiler();
      profilerfiller.push("poi");
      this.poiManager.tick(pHasMoreTime);
      profilerfiller.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(pHasMoreTime);
      }

      profilerfiller.pop();
   }

   public boolean hasWork() {
      return this.lightEngine.hasLightWork() || !this.pendingUnloads.isEmpty() || !this.updatingChunkMap.isEmpty() || this.poiManager.hasWork() || !this.toDrop.isEmpty() || !this.unloadQueue.isEmpty() || this.queueSorter.hasWork() || this.distanceManager.hasTickets();
   }

   private void processUnloads(BooleanSupplier pHasMoreTime) {
      LongIterator longiterator = this.toDrop.iterator();

      for(int i = 0; longiterator.hasNext() && (pHasMoreTime.getAsBoolean() || i < 200 || this.toDrop.size() > 2000); longiterator.remove()) {
         long j = longiterator.nextLong();
         ChunkHolder chunkholder = this.updatingChunkMap.remove(j);
         if (chunkholder != null) {
            this.pendingUnloads.put(j, chunkholder);
            this.modified = true;
            ++i;
            this.scheduleUnload(j, chunkholder);
         }
      }

      int l = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable runnable;
      while((pHasMoreTime.getAsBoolean() || l > 0) && (runnable = this.unloadQueue.poll()) != null) {
         --l;
         runnable.run();
      }

      int k = 0;
      ObjectIterator<ChunkHolder> objectiterator = this.visibleChunkMap.values().iterator();

      while(k < 20 && pHasMoreTime.getAsBoolean() && objectiterator.hasNext()) {
         if (this.saveChunkIfNeeded(objectiterator.next())) {
            ++k;
         }
      }

   }

   private void scheduleUnload(long pChunkPos, ChunkHolder pChunkHolder) {
      CompletableFuture<ChunkAccess> completablefuture = pChunkHolder.getChunkToSave();
      completablefuture.thenAcceptAsync((p_202998_5_) -> {
         CompletableFuture<ChunkAccess> completablefuture1 = pChunkHolder.getChunkToSave();
         if (completablefuture1 != completablefuture) {
            this.scheduleUnload(pChunkPos, pChunkHolder);
         } else if (this.pendingUnloads.remove(pChunkPos, pChunkHolder) && p_202998_5_ != null) {
            if (p_202998_5_ instanceof LevelChunk) {
               ((LevelChunk)p_202998_5_).setLoaded(false);
               if (Reflector.ForgeEventFactory_onChunkUnload.exists()) {
                  Reflector.ForgeEventFactory_onChunkUnload.call((Object)p_202998_5_);
               }
            }

            this.save(p_202998_5_);
            if (this.entitiesInLevel.remove(pChunkPos) && p_202998_5_ instanceof LevelChunk) {
               LevelChunk levelchunk = (LevelChunk)p_202998_5_;
               this.level.unload(levelchunk);
            }

            this.lightEngine.updateChunkStatus(p_202998_5_.getPos());
            this.lightEngine.tryScheduleUpdate();
            this.progressListener.onStatusChange(p_202998_5_.getPos(), (ChunkStatus)null);
            this.chunkSaveCooldowns.remove(p_202998_5_.getPos().toLong());
         }

      }, this.unloadQueue::add).whenComplete((p_202994_1_, p_202994_2_) -> {
         if (p_202994_2_ != null) {
            LOGGER.error("Failed to save chunk {}", pChunkHolder.getPos(), p_202994_2_);
         }

      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      } else {
         this.visibleChunkMap = this.updatingChunkMap.clone();
         this.modified = false;
         return true;
      }
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder pHolder, ChunkStatus pStatus) {
      ChunkPos chunkpos = pHolder.getPos();
      if (pStatus == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(chunkpos);
      } else {
         if (pStatus == ChunkStatus.LIGHT) {
            this.distanceManager.addTicket(TicketType.LIGHT, chunkpos, ChunkLevel.byStatus(ChunkStatus.LIGHT), chunkpos);
         }

         if (!pStatus.hasLoadDependencies()) {
            Optional<ChunkAccess> optional = pHolder.getOrScheduleFuture(pStatus.getParent(), this).getNow(ChunkHolder.UNLOADED_CHUNK).left();
            if (optional.isPresent() && optional.get().getStatus().isOrAfter(pStatus)) {
               CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = pStatus.load(this.level, this.structureTemplateManager, this.lightEngine, (p_203079_2_) -> {
                  return this.protoChunkToFullChunk(pHolder);
               }, optional.get());
               this.progressListener.onStatusChange(chunkpos, pStatus);
               return completablefuture;
            }
         }

         return this.scheduleChunkGeneration(pHolder, pStatus);
      }
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos pChunkPos) {
      return this.readChunk(pChunkPos).thenApply((p_214923_1_) -> {
         return p_214923_1_.filter((p_214926_1_) -> {
            boolean flag = isChunkDataValid(p_214926_1_);
            if (!flag) {
               LOGGER.error("Chunk file at {} is missing level data, skipping", (Object)pChunkPos);
            }

            return flag;
         });
      }).<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>thenApplyAsync((p_304410_2_) -> {
         this.level.getProfiler().incrementCounter("chunkLoad");
         if (p_304410_2_.isPresent()) {
            ChunkAccess chunkaccess = ChunkSerializer.read(this.level, this.poiManager, pChunkPos, p_304410_2_.get());
            this.markPosition(pChunkPos, chunkaccess.getStatus().getChunkType());
            return Either.left(chunkaccess);
         } else {
            return Either.left(this.createEmptyChunk(pChunkPos));
         }
      }, this.mainThreadExecutor).exceptionallyAsync((p_214886_2_) -> {
         return this.handleChunkLoadFailure(p_214886_2_, pChunkPos);
      }, this.mainThreadExecutor);
   }

   private static boolean isChunkDataValid(CompoundTag pTag) {
      return pTag.contains("Status", 8);
   }

   private Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> handleChunkLoadFailure(Throwable pException, ChunkPos pChunkPos) {
      if (pException instanceof ReportedException reportedexception) {
         Throwable throwable = reportedexception.getCause();
         if (!(throwable instanceof IOException)) {
            this.markPositionReplaceable(pChunkPos);
            throw reportedexception;
         }

         LOGGER.error("Couldn't load chunk {}", pChunkPos, throwable);
      } else if (pException instanceof IOException) {
         LOGGER.error("Couldn't load chunk {}", pChunkPos, pException);
      }

      return Either.left(this.createEmptyChunk(pChunkPos));
   }

   private ChunkAccess createEmptyChunk(ChunkPos pChunkPos) {
      this.markPositionReplaceable(pChunkPos);
      return new ProtoChunk(pChunkPos, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registries.BIOME), (BlendingData)null);
   }

   private void markPositionReplaceable(ChunkPos pChunkPos) {
      this.chunkTypeCache.put(pChunkPos.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos pChunkPos, ChunkStatus.ChunkType pChunkType) {
      return this.chunkTypeCache.put(pChunkPos.toLong(), (byte)(pChunkType == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder pChunkHolder, ChunkStatus pChunkStatus) {
      ChunkPos chunkpos = pChunkHolder.getPos();
      CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkRangeFuture(pChunkHolder, pChunkStatus.getRange(), (p_214933_2_) -> {
         return this.getDependencyStatus(pChunkStatus, p_214933_2_);
      });
      this.level.getProfiler().incrementCounter(() -> {
         return "chunkGenerate " + pChunkStatus;
      });
      Executor executor = (p_214956_2_) -> {
         this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(pChunkHolder, p_214956_2_));
      };
      return completablefuture.thenComposeAsync((p_214868_5_) -> {
         return p_214868_5_.map((p_279891_5_) -> {
            try {
               ChunkAccess chunkaccess = p_279891_5_.get(p_279891_5_.size() / 2);
               CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture1;
               if (chunkaccess.getStatus().isOrAfter(pChunkStatus)) {
                  completablefuture1 = pChunkStatus.load(this.level, this.structureTemplateManager, this.lightEngine, (p_214917_2_) -> {
                     return this.protoChunkToFullChunk(pChunkHolder);
                  }, chunkaccess);
               } else {
                  completablefuture1 = pChunkStatus.generate(executor, this.level, this.generator, this.structureTemplateManager, this.lightEngine, (p_279890_2_) -> {
                     return this.protoChunkToFullChunk(pChunkHolder);
                  }, p_279891_5_);
               }

               this.progressListener.onStatusChange(chunkpos, pChunkStatus);
               return completablefuture1;
            } catch (Exception exception1) {
               exception1.getStackTrace();
               CrashReport crashreport = CrashReport.forThrowable(exception1, "Exception generating new chunk");
               CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
               crashreportcategory.setDetail("Location", String.format(Locale.ROOT, "%d,%d", chunkpos.x, chunkpos.z));
               crashreportcategory.setDetail("Position hash", ChunkPos.asLong(chunkpos.x, chunkpos.z));
               crashreportcategory.setDetail("Generator", this.generator);
               this.mainThreadExecutor.execute(() -> {
                  throw new ReportedException(crashreport);
               });
               throw new ReportedException(crashreport);
            }
         }, (p_214865_2_) -> {
            this.releaseLightTicket(chunkpos);
            return CompletableFuture.completedFuture(Either.right(p_214865_2_));
         });
      }, executor);
   }

   protected void releaseLightTicket(ChunkPos pChunkPos) {
      this.mainThreadExecutor.tell(Util.name(() -> {
         this.distanceManager.removeTicket(TicketType.LIGHT, pChunkPos, ChunkLevel.byStatus(ChunkStatus.LIGHT), pChunkPos);
      }, () -> {
         return "release light ticket " + pChunkPos;
      }));
   }

   private ChunkStatus getDependencyStatus(ChunkStatus pChunkStatus, int p_140264_) {
      ChunkStatus chunkstatus;
      if (p_140264_ == 0) {
         chunkstatus = pChunkStatus.getParent();
      } else {
         chunkstatus = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(pChunkStatus) + p_140264_);
      }

      return chunkstatus;
   }

   private static void postLoadProtoChunk(ServerLevel pLevel, List<CompoundTag> pTags) {
      if (!pTags.isEmpty()) {
         pLevel.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(pTags, pLevel));
      }

   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder pHolder) {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = pHolder.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
      return completablefuture.thenApplyAsync((p_287044_2_) -> {
         ChunkStatus chunkstatus = ChunkLevel.generationStatus(pHolder.getTicketLevel());
         return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : p_287044_2_.mapLeft((p_214854_2_) -> {
            ChunkPos chunkpos = pHolder.getPos();
            ProtoChunk protochunk = (ProtoChunk)p_214854_2_;
            LevelChunk levelchunk;
            if (protochunk instanceof ImposterProtoChunk) {
               levelchunk = ((ImposterProtoChunk)protochunk).getWrapped();
            } else {
               levelchunk = new LevelChunk(this.level, protochunk, (p_214898_2_) -> {
                  postLoadProtoChunk(this.level, protochunk.getEntities());
               });
               pHolder.replaceProtoChunk(new ImposterProtoChunk(levelchunk, false));
            }

            levelchunk.setFullStatus(() -> {
               return ChunkLevel.fullStatus(pHolder.getTicketLevel());
            });
            levelchunk.runPostLoad();
            if (this.entitiesInLevel.add(chunkpos.toLong())) {
               levelchunk.setLoaded(true);

               try {
                  Reflector.setFieldValue(pHolder, Reflector.ForgeChunkHolder_currentlyLoading, levelchunk);
                  levelchunk.registerAllBlockEntitiesAfterLevelLoad();
                  levelchunk.registerTickContainerInLevel(this.level);
                  if (Reflector.ForgeEventFactory_onChunkLoad.exists()) {
                     Reflector.ForgeEventFactory_onChunkLoad.call(levelchunk, !(protochunk instanceof ImposterProtoChunk));
                  }
               } finally {
                  Reflector.setFieldValue(pHolder, Reflector.ForgeChunkHolder_currentlyLoading, (Object)null);
               }
            }

            return levelchunk;
         });
      }, (p_214949_2_) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(p_214949_2_, pHolder.getPos().toLong(), pHolder::getTicketLevel));
      });
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder pHolder) {
      CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkRangeFuture(pHolder, 1, (p_214915_0_) -> {
         return ChunkStatus.FULL;
      });
      CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = completablefuture.thenApplyAsync((p_279889_0_) -> {
         return p_279889_0_.mapLeft((p_292223_0_) -> {
            return (LevelChunk)p_292223_0_.get(p_292223_0_.size() / 2);
         });
      }, (p_214942_2_) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(pHolder, p_214942_2_));
      }).thenApplyAsync((p_292224_2_) -> {
         return p_292224_2_.ifLeft((p_292218_2_) -> {
            p_292218_2_.postProcessGeneration();
            this.level.startTickingChunk(p_292218_2_);
            CompletableFuture<?> completablefuture2 = pHolder.getChunkSendSyncFuture();
            if (completablefuture2.isDone()) {
               this.onChunkReadyToSend(p_292218_2_);
            } else {
               completablefuture2.thenAcceptAsync((p_292222_2_) -> {
                  this.onChunkReadyToSend(p_292218_2_);
               }, this.mainThreadExecutor);
            }

         });
      }, this.mainThreadExecutor);
      completablefuture1.handle((p_287045_1_, p_287045_2_) -> {
         this.tickingGenerated.getAndIncrement();
         return null;
      });
      return completablefuture1;
   }

   private void onChunkReadyToSend(LevelChunk p_299599_) {
      ChunkPos chunkpos = p_299599_.getPos();

      for(ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         if (serverplayer.getChunkTrackingView().contains(chunkpos)) {
            markChunkPendingToSend(serverplayer, p_299599_);
         }
      }

   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder pHolder) {
      return this.getChunkRangeFuture(pHolder, 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((p_203085_0_) -> {
         return p_203085_0_.mapLeft((p_203091_0_) -> {
            return (LevelChunk)p_203091_0_.get(p_203091_0_.size() / 2);
         });
      }, (p_214857_2_) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(pHolder, p_214857_2_));
      });
   }

   public int getTickingGenerated() {
      return this.tickingGenerated.get();
   }

   private boolean saveChunkIfNeeded(ChunkHolder p_198875_) {
      if (!p_198875_.wasAccessibleSinceLastSave()) {
         return false;
      } else {
         ChunkAccess chunkaccess = p_198875_.getChunkToSave().getNow((ChunkAccess)null);
         if (!(chunkaccess instanceof ImposterProtoChunk) && !(chunkaccess instanceof LevelChunk)) {
            return false;
         } else {
            long i = chunkaccess.getPos().toLong();
            long j = this.chunkSaveCooldowns.getOrDefault(i, -1L);
            long k = System.currentTimeMillis();
            if (k < j) {
               return false;
            } else {
               boolean flag = this.save(chunkaccess);
               p_198875_.refreshAccessibility();
               if (flag) {
                  this.chunkSaveCooldowns.put(i, k + 10000L);
               }

               return flag;
            }
         }
      }
   }

   private boolean save(ChunkAccess p_140259_) {
      this.poiManager.flush(p_140259_.getPos());
      if (!p_140259_.isUnsaved()) {
         return false;
      } else {
         p_140259_.setUnsaved(false);
         ChunkPos chunkpos = p_140259_.getPos();

         try {
            ChunkStatus chunkstatus = p_140259_.getStatus();
            if (chunkstatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
               if (this.isExistingChunkFull(chunkpos)) {
                  return false;
               }

               if (chunkstatus == ChunkStatus.EMPTY && p_140259_.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                  return false;
               }
            }

            this.level.getProfiler().incrementCounter("chunkSave");
            CompoundTag compoundtag = ChunkSerializer.write(this.level, p_140259_);
            if (Reflector.ForgeEventFactory_onChunkDataSave.exists()) {
               Level level = (Level)Reflector.call(p_140259_, Reflector.ForgeIChunk_getWorldForge);
               Reflector.ForgeEventFactory_onChunkDataSave.call(p_140259_, level != null ? level : this.level, compoundtag);
            }

            this.write(chunkpos, compoundtag);
            this.markPosition(chunkpos, chunkstatus.getChunkType());
            return true;
         } catch (Exception exception1) {
            LOGGER.error("Failed to save chunk {},{}", chunkpos.x, chunkpos.z, exception1);
            return false;
         }
      }
   }

   private boolean isExistingChunkFull(ChunkPos pChunkPos) {
      byte b0 = this.chunkTypeCache.get(pChunkPos.toLong());
      if (b0 != 0) {
         return b0 == 1;
      } else {
         CompoundTag compoundtag;
         try {
            compoundtag = this.readChunk(pChunkPos).join().orElse((CompoundTag)null);
            if (compoundtag == null) {
               this.markPositionReplaceable(pChunkPos);
               return false;
            }
         } catch (Exception exception) {
            LOGGER.error("Failed to read chunk {}", pChunkPos, exception);
            this.markPositionReplaceable(pChunkPos);
            return false;
         }

         ChunkStatus.ChunkType chunkstatus$chunktype = ChunkSerializer.getChunkTypeFromTag(compoundtag);
         return this.markPosition(pChunkPos, chunkstatus$chunktype) == 1;
      }
   }

   protected void setServerViewDistance(int pViewDistance) {
      int i = Mth.clamp(pViewDistance, 2, 64);
      if (i != this.serverViewDistance) {
         this.serverViewDistance = i;
         this.distanceManager.updatePlayerTickets(this.serverViewDistance);

         for(ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            this.updateChunkTracking(serverplayer);
         }
      }

   }

   int getPlayerViewDistance(ServerPlayer pPlayer) {
      return Mth.clamp(pPlayer.requestedViewDistance(), 2, this.serverViewDistance);
   }

   private void markChunkPendingToSend(ServerPlayer pPlayer, ChunkPos pChunkPos) {
      LevelChunk levelchunk = this.getChunkToSend(pChunkPos.toLong());
      if (levelchunk != null) {
         markChunkPendingToSend(pPlayer, levelchunk);
      }

   }

   private static void markChunkPendingToSend(ServerPlayer pPlayer, LevelChunk pChunk) {
      pPlayer.connection.chunkSender.markChunkPendingToSend(pChunk);
   }

   private static void dropChunk(ServerPlayer pPlayer, ChunkPos pChunkPos) {
      pPlayer.connection.chunkSender.dropChunk(pPlayer, pChunkPos);
   }

   @Nullable
   public LevelChunk getChunkToSend(long pChunkPos) {
      ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
      return chunkholder == null ? null : chunkholder.getChunkToSend();
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   protected Iterable<ChunkHolder> getChunks() {
      return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
   }

   void dumpChunks(Writer pWriter) throws IOException {
      CsvOutput csvoutput = CsvOutput.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(pWriter);
      TickingTracker tickingtracker = this.distanceManager.tickingTracker();

      for(Long2ObjectMap.Entry<ChunkHolder> entry : this.visibleChunkMap.long2ObjectEntrySet()) {
         long i = entry.getLongKey();
         ChunkPos chunkpos = new ChunkPos(i);
         ChunkHolder chunkholder = entry.getValue();
         Optional<ChunkAccess> optional = Optional.ofNullable(chunkholder.getLastAvailable());
         Optional<LevelChunk> optional1 = optional.flatMap((p_214931_0_) -> {
            return p_214931_0_ instanceof LevelChunk ? Optional.of((LevelChunk)p_214931_0_) : Optional.empty();
         });
         csvoutput.writeRow(chunkpos.x, chunkpos.z, chunkholder.getTicketLevel(), optional.isPresent(), optional.map(ChunkAccess::getStatus).orElse((ChunkStatus)null), optional1.map(LevelChunk::getFullStatus).orElse((FullChunkStatus)null), printFuture(chunkholder.getFullChunkFuture()), printFuture(chunkholder.getTickingChunkFuture()), printFuture(chunkholder.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(i), this.anyPlayerCloseEnoughForSpawning(chunkpos), optional1.map((p_214952_0_) -> {
            return p_214952_0_.getBlockEntities().size();
         }).orElse(0), tickingtracker.getTicketDebugString(i), tickingtracker.getLevel(i), optional1.map((p_214945_0_) -> {
            return p_214945_0_.getBlockTicks().count();
         }).orElse(0), optional1.map((p_214936_0_) -> {
            return p_214936_0_.getFluidTicks().count();
         }).orElse(0));
      }

   }

   private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> pFuture) {
      try {
         Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = pFuture.getNow((Either)null);
         return either != null ? either.map((p_214896_0_) -> {
            return "done";
         }, (p_214849_0_) -> {
            return "unloaded";
         }) : "not completed";
      } catch (CompletionException completionexception) {
         return "failed " + completionexception.getCause().getMessage();
      } catch (CancellationException cancellationexception1) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos pPos) {
      return this.read(pPos).thenApplyAsync((p_214906_1_) -> {
         return p_214906_1_.map(this::upgradeChunkTag);
      }, Util.backgroundExecutor());
   }

   private CompoundTag upgradeChunkTag(CompoundTag p_214948_) {
      return this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, p_214948_, this.generator.getTypeNameForDataFixer());
   }

   boolean anyPlayerCloseEnoughForSpawning(ChunkPos pChunkPos) {
      if (!this.distanceManager.hasPlayersNearby(pChunkPos.toLong())) {
         return false;
      } else {
         for(ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            if (this.playerIsCloseEnoughForSpawning(serverplayer, pChunkPos)) {
               return true;
            }
         }

         return false;
      }
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos pChunkPos) {
      long i = pChunkPos.toLong();
      if (!this.distanceManager.hasPlayersNearby(i)) {
         return List.of();
      } else {
         ImmutableList.Builder<ServerPlayer> builder = ImmutableList.builder();

         for(ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            if (this.playerIsCloseEnoughForSpawning(serverplayer, pChunkPos)) {
               builder.add(serverplayer);
            }
         }

         return builder.build();
      }
   }

   private boolean playerIsCloseEnoughForSpawning(ServerPlayer pPlayer, ChunkPos pChunkPos) {
      if (pPlayer.isSpectator()) {
         return false;
      } else {
         double d0 = euclideanDistanceSquared(pChunkPos, pPlayer);
         return d0 < 16384.0D;
      }
   }

   private boolean skipPlayer(ServerPlayer pPlayer) {
      return pPlayer.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
   }

   void updatePlayerStatus(ServerPlayer pPlayer, boolean pTrack) {
      boolean flag = this.skipPlayer(pPlayer);
      boolean flag1 = this.playerMap.ignoredOrUnknown(pPlayer);
      if (pTrack) {
         this.playerMap.addPlayer(pPlayer, flag);
         this.updatePlayerPos(pPlayer);
         if (!flag) {
            this.distanceManager.addPlayer(SectionPos.of(pPlayer), pPlayer);
         }

         pPlayer.setChunkTrackingView(ChunkTrackingView.EMPTY);
         this.updateChunkTracking(pPlayer);
      } else {
         SectionPos sectionpos = pPlayer.getLastSectionPos();
         this.playerMap.removePlayer(pPlayer);
         if (!flag1) {
            this.distanceManager.removePlayer(sectionpos, pPlayer);
         }

         this.applyChunkTrackingView(pPlayer, ChunkTrackingView.EMPTY);
      }

   }

   private void updatePlayerPos(ServerPlayer pPlayer) {
      SectionPos sectionpos = SectionPos.of(pPlayer);
      pPlayer.setLastSectionPos(sectionpos);
   }

   public void move(ServerPlayer pPlayer) {
      for(ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
         if (chunkmap$trackedentity.entity == pPlayer) {
            chunkmap$trackedentity.updatePlayers(this.level.players());
         } else {
            chunkmap$trackedentity.updatePlayer(pPlayer);
         }
      }

      SectionPos sectionpos = pPlayer.getLastSectionPos();
      SectionPos sectionpos1 = SectionPos.of(pPlayer);
      boolean flag = this.playerMap.ignored(pPlayer);
      boolean flag1 = this.skipPlayer(pPlayer);
      boolean flag2 = sectionpos.asLong() != sectionpos1.asLong();
      if (flag2 || flag != flag1) {
         this.updatePlayerPos(pPlayer);
         if (!flag) {
            this.distanceManager.removePlayer(sectionpos, pPlayer);
         }

         if (!flag1) {
            this.distanceManager.addPlayer(sectionpos1, pPlayer);
         }

         if (!flag && flag1) {
            this.playerMap.ignorePlayer(pPlayer);
         }

         if (flag && !flag1) {
            this.playerMap.unIgnorePlayer(pPlayer);
         }

         this.updateChunkTracking(pPlayer);
      }

   }

   private void updateChunkTracking(ServerPlayer pPlayer) {
      ChunkPos chunkpos = pPlayer.chunkPosition();
      int i = this.getPlayerViewDistance(pPlayer);
      ChunkTrackingView chunktrackingview = pPlayer.getChunkTrackingView();
      if (chunktrackingview instanceof ChunkTrackingView.Positioned chunktrackingview$positioned) {
         if (chunktrackingview$positioned.center().equals(chunkpos) && chunktrackingview$positioned.viewDistance() == i) {
            return;
         }
      }

      this.applyChunkTrackingView(pPlayer, ChunkTrackingView.of(chunkpos, i));
   }

   private void applyChunkTrackingView(ServerPlayer pPlayer, ChunkTrackingView pChunkTrackingView) {
      if (pPlayer.level() == this.level) {
         ChunkTrackingView chunktrackingview = pPlayer.getChunkTrackingView();
         if (pChunkTrackingView instanceof ChunkTrackingView.Positioned) {
            label22: {
               ChunkTrackingView.Positioned chunktrackingview$positioned = (ChunkTrackingView.Positioned)pChunkTrackingView;
               if (chunktrackingview instanceof ChunkTrackingView.Positioned) {
                  ChunkTrackingView.Positioned chunktrackingview$positioned1 = (ChunkTrackingView.Positioned)chunktrackingview;
                  if (chunktrackingview$positioned1.center().equals(chunktrackingview$positioned.center())) {
                     break label22;
                  }
               }

               pPlayer.connection.send(new ClientboundSetChunkCacheCenterPacket(chunktrackingview$positioned.center().x, chunktrackingview$positioned.center().z));
            }
         }

         ChunkTrackingView.difference(chunktrackingview, pChunkTrackingView, (p_292219_2_) -> {
            this.markChunkPendingToSend(pPlayer, p_292219_2_);
         }, (p_292220_1_) -> {
            dropChunk(pPlayer, p_292220_1_);
         });
         pPlayer.setChunkTrackingView(pChunkTrackingView);
      }

   }

   public List<ServerPlayer> getPlayers(ChunkPos pPos, boolean pBoundaryOnly) {
      Set<ServerPlayer> set = this.playerMap.getAllPlayers();
      ImmutableList.Builder<ServerPlayer> builder = ImmutableList.builder();

      for(ServerPlayer serverplayer : set) {
         if (pBoundaryOnly && this.isChunkOnTrackedBorder(serverplayer, pPos.x, pPos.z) || !pBoundaryOnly && this.isChunkTracked(serverplayer, pPos.x, pPos.z)) {
            builder.add(serverplayer);
         }
      }

      return builder.build();
   }

   protected void addEntity(Entity pEntity) {
      boolean flag = pEntity instanceof EnderDragonPart;
      if (Reflector.PartEntity.exists()) {
         flag = Reflector.PartEntity.isInstance(pEntity);
      }

      if (!flag) {
         EntityType<?> entitytype = pEntity.getType();
         int i = entitytype.clientTrackingRange() * 16;
         if (i != 0) {
            int j = entitytype.updateInterval();
            if (this.entityMap.containsKey(pEntity.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            }

            ChunkMap.TrackedEntity chunkmap$trackedentity = new ChunkMap.TrackedEntity(pEntity, i, j, entitytype.trackDeltas());
            this.entityMap.put(pEntity.getId(), chunkmap$trackedentity);
            chunkmap$trackedentity.updatePlayers(this.level.players());
            if (pEntity instanceof ServerPlayer) {
               ServerPlayer serverplayer = (ServerPlayer)pEntity;
               this.updatePlayerStatus(serverplayer, true);

               for(ChunkMap.TrackedEntity chunkmap$trackedentity1 : this.entityMap.values()) {
                  if (chunkmap$trackedentity1.entity != serverplayer) {
                     chunkmap$trackedentity1.updatePlayer(serverplayer);
                  }
               }
            }
         }
      }

   }

   protected void removeEntity(Entity pEntity) {
      if (pEntity instanceof ServerPlayer serverplayer) {
         this.updatePlayerStatus(serverplayer, false);

         for(ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
            chunkmap$trackedentity.removePlayer(serverplayer);
         }
      }

      ChunkMap.TrackedEntity chunkmap$trackedentity1 = this.entityMap.remove(pEntity.getId());
      if (chunkmap$trackedentity1 != null) {
         chunkmap$trackedentity1.broadcastRemoved();
      }

   }

   protected void tick() {
      for(ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
         this.updateChunkTracking(serverplayer);
      }

      List<ServerPlayer> list = Lists.newArrayList();
      List<ServerPlayer> list1 = this.level.players();

      for(ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
         SectionPos sectionpos = chunkmap$trackedentity.lastSectionPos;
         SectionPos sectionpos1 = SectionPos.of(chunkmap$trackedentity.entity);
         boolean flag = !Objects.equals(sectionpos, sectionpos1);
         if (flag) {
            chunkmap$trackedentity.updatePlayers(list1);
            Entity entity = chunkmap$trackedentity.entity;
            if (entity instanceof ServerPlayer) {
               list.add((ServerPlayer)entity);
            }

            chunkmap$trackedentity.lastSectionPos = sectionpos1;
         }

         if (flag || this.distanceManager.inEntityTickingRange(sectionpos1.chunk().toLong())) {
            chunkmap$trackedentity.serverEntity.sendChanges();
         }
      }

      if (!list.isEmpty()) {
         for(ChunkMap.TrackedEntity chunkmap$trackedentity1 : this.entityMap.values()) {
            chunkmap$trackedentity1.updatePlayers(list);
         }
      }

   }

   public void broadcast(Entity pEntity, Packet<?> pPacket) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = this.entityMap.get(pEntity.getId());
      if (chunkmap$trackedentity != null) {
         chunkmap$trackedentity.broadcast(pPacket);
      }

   }

   protected void broadcastAndSend(Entity pEntity, Packet<?> pPacket) {
      ChunkMap.TrackedEntity chunkmap$trackedentity = this.entityMap.get(pEntity.getId());
      if (chunkmap$trackedentity != null) {
         chunkmap$trackedentity.broadcastAndSend(pPacket);
      }

   }

   public void resendBiomesForChunks(List<ChunkAccess> pChunks) {
      Map<ServerPlayer, List<LevelChunk>> map = new HashMap<>();

      for(ChunkAccess chunkaccess : pChunks) {
         ChunkPos chunkpos = chunkaccess.getPos();
         LevelChunk levelchunk;
         if (chunkaccess instanceof LevelChunk levelchunk1) {
            levelchunk = levelchunk1;
         } else {
            levelchunk = this.level.getChunk(chunkpos.x, chunkpos.z);
         }

         for(ServerPlayer serverplayer : this.getPlayers(chunkpos, false)) {
            map.computeIfAbsent(serverplayer, (p_274108_0_) -> {
               return new ArrayList();
            }).add(levelchunk);
         }
      }

      map.forEach((p_292221_0_, p_292221_1_) -> {
         p_292221_0_.connection.send(ClientboundChunksBiomesPacket.forChunks(p_292221_1_));
      });
   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public String getStorageName() {
      return this.storageName;
   }

   void onFullChunkStatusChange(ChunkPos pChunkPos, FullChunkStatus pFullChunkStatus) {
      this.chunkStatusListener.onChunkStatusChange(pChunkPos, pFullChunkStatus);
   }

   public void waitForLightBeforeSending(ChunkPos pChunkPos, int p_300649_) {
      int i = p_300649_ + 1;
      ChunkPos.rangeClosed(pChunkPos, i).forEach((p_292225_1_) -> {
         ChunkHolder chunkholder = this.getVisibleChunkIfPresent(p_292225_1_.toLong());
         if (chunkholder != null) {
            chunkholder.addSendDependency(this.lightEngine.waitForPendingTasks(p_292225_1_.x, p_292225_1_.z));
         }

      });
   }

   class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(Executor pDispatcher, Executor pMainThreadExecutor) {
         super(pDispatcher, pMainThreadExecutor);
      }

      protected boolean isChunkToRemove(long pChunkPos) {
         return ChunkMap.this.toDrop.contains(pChunkPos);
      }

      @Nullable
      protected ChunkHolder getChunk(long pChunkPos) {
         return ChunkMap.this.getUpdatingChunkIfPresent(pChunkPos);
      }

      @Nullable
      protected ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel) {
         return ChunkMap.this.updateChunkScheduling(pChunkPos, pNewLevel, pHolder, pOldLevel);
      }
   }

   class TrackedEntity {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(Entity pEntity, int pRange, int pUpdateInterval, boolean pTrackDelta) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, pEntity, pUpdateInterval, pTrackDelta, this::broadcast);
         this.entity = pEntity;
         this.range = pRange;
         this.lastSectionPos = SectionPos.of(pEntity);
      }

      public boolean equals(Object pOther) {
         if (pOther instanceof ChunkMap.TrackedEntity) {
            return ((ChunkMap.TrackedEntity)pOther).entity.getId() == this.entity.getId();
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.entity.getId();
      }

      public void broadcast(Packet<?> p_140490_) {
         for(ServerPlayerConnection serverplayerconnection : this.seenBy) {
            serverplayerconnection.send(p_140490_);
         }

      }

      public void broadcastAndSend(Packet<?> pPacket) {
         this.broadcast(pPacket);
         if (this.entity instanceof ServerPlayer) {
            ((ServerPlayer)this.entity).connection.send(pPacket);
         }

      }

      public void broadcastRemoved() {
         for(ServerPlayerConnection serverplayerconnection : this.seenBy) {
            this.serverEntity.removePairing(serverplayerconnection.getPlayer());
         }

      }

      public void removePlayer(ServerPlayer pPlayer) {
         if (this.seenBy.remove(pPlayer.connection)) {
            this.serverEntity.removePairing(pPlayer);
         }

      }

      public void updatePlayer(ServerPlayer pPlayer) {
         if (pPlayer != this.entity) {
            Vec3 vec3 = pPlayer.position().subtract(this.entity.position());
            int i = ChunkMap.this.getPlayerViewDistance(pPlayer);
            double d0 = (double)Math.min(this.getEffectiveRange(), i * 16);
            double d1 = vec3.x * vec3.x + vec3.z * vec3.z;
            double d2 = d0 * d0;
            boolean flag = d1 <= d2 && this.entity.broadcastToPlayer(pPlayer) && ChunkMap.this.isChunkTracked(pPlayer, this.entity.chunkPosition().x, this.entity.chunkPosition().z);
            if (flag) {
               if (this.seenBy.add(pPlayer.connection)) {
                  this.serverEntity.addPairing(pPlayer);
               }
            } else if (this.seenBy.remove(pPlayer.connection)) {
               this.serverEntity.removePairing(pPlayer);
            }
         }

      }

      private int scaledRange(int pTrackingDistance) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(pTrackingDistance);
      }

      private int getEffectiveRange() {
         int i = this.range;
         if (!this.entity.getPassengers().isEmpty()) {
            for(Entity entity : this.entity.getIndirectPassengers()) {
               int j = entity.getType().clientTrackingRange() * 16;
               if (j > i) {
                  i = j;
               }
            }
         }

         return this.scaledRange(i);
      }

      public void updatePlayers(List<ServerPlayer> pPlayersList) {
         for(ServerPlayer serverplayer : pPlayersList) {
            this.updatePlayer(serverplayer);
         }

      }
   }
}
