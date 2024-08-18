package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomBlockLayers;
import net.optifine.override.ChunkCacheOF;
import net.optifine.reflect.Reflector;
import net.optifine.render.AabbFrame;
import net.optifine.render.ChunkLayerMap;
import net.optifine.render.ChunkLayerSet;
import net.optifine.render.ICamera;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderTypes;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;
import net.optifine.util.ChunkUtils;
import net.optifine.util.SingleIterable;

public class SectionRenderDispatcher {
   private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
   private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
   private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
   private int highPriorityQuota = 2;
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   private volatile int toBatchCount;
   private volatile boolean closed;
   private final ProcessorMailbox<Runnable> mailbox;
   private final Executor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   private Vec3 camera = Vec3.ZERO;
   private int countRenderBuilders;
   private List<SectionBufferBuilderPack> listPausedBuilders = new ArrayList<>();
   public static final RenderType[] BLOCK_RENDER_LAYERS = RenderType.chunkBufferLayers().toArray(new RenderType[0]);
   public static final RenderType[] BLOCK_RENDER_LAYERS_FORGE = RenderType.chunkBufferLayers().toArray(new RenderType[0]);
   private static final boolean FORGE = Reflector.ForgeHooksClient.exists();
   public static int renderChunksUpdated;

   public SectionRenderDispatcher(ClientLevel pLevel, LevelRenderer pRenderer, Executor pExecutor, RenderBuffers pRenderBuffers) {
      this.level = pLevel;
      this.renderer = pRenderer;
      this.fixedBuffers = pRenderBuffers.fixedBufferPack();
      this.bufferPool = pRenderBuffers.sectionBufferPool();
      this.countRenderBuilders = this.bufferPool.getFreeBufferCount();
      this.executor = pExecutor;
      this.mailbox = ProcessorMailbox.create(pExecutor, "Section Renderer");
      this.mailbox.tell(this::runTask);
   }

   public void setLevel(ClientLevel pLevel) {
      this.level = pLevel;
   }

   private void runTask() {
      if (!this.closed && !this.bufferPool.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.pollTask();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
            if (sectionbufferbuilderpack == null) {
               this.toBatchHighPriority.add(sectionrenderdispatcher$rendersection$compiletask);
               return;
            }

            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName(sectionrenderdispatcher$rendersection$compiletask.name(), () -> {
               return sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack);
            }), this.executor).thenCompose((resultIn) -> {
               return resultIn;
            }).whenComplete((taskResultIn, throwableIn) -> {
               if (throwableIn != null) {
                  Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Batching sections"));
               } else {
                  this.mailbox.tell(() -> {
                     if (taskResultIn == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                        sectionbufferbuilderpack.clearAll();
                     } else {
                        sectionbufferbuilderpack.discardAll();
                     }

                     this.bufferPool.release(sectionbufferbuilderpack);
                     this.runTask();
                  });
               }

            });
         }
      }

   }

   @Nullable
   private SectionRenderDispatcher.RenderSection.CompileTask pollTask() {
      if (this.highPriorityQuota <= 0) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchLowPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            this.highPriorityQuota = 2;
            return sectionrenderdispatcher$rendersection$compiletask;
         }
      }

      SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchHighPriority.poll();
      if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
         --this.highPriorityQuota;
         return sectionrenderdispatcher$rendersection$compiletask1;
      } else {
         this.highPriorityQuota = 2;
         return this.toBatchLowPriority.poll();
      }
   }

   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
   }

   public int getToBatchCount() {
      return this.toBatchCount;
   }

   public int getToUpload() {
      return this.toUpload.size();
   }

   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   public void setCamera(Vec3 pCamera) {
      this.camera = pCamera;
   }

   public Vec3 getCameraPosition() {
      return this.camera;
   }

   public void uploadAllPendingUploads() {
      Runnable runnable;
      while((runnable = this.toUpload.poll()) != null) {
         runnable.run();
      }

   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
      pSection.compileSync(pRegionCache);
   }

   public void blockUntilClear() {
      this.clearBatchQueue();
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
      if (!this.closed) {
         this.mailbox.tell(() -> {
            if (!this.closed) {
               if (pTask.isHighPriority) {
                  this.toBatchHighPriority.offer(pTask);
               } else {
                  this.toBatchLowPriority.offer(pTask);
               }

               this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
               this.runTask();
            }

         });
      }

   }

   public CompletableFuture<Void> uploadSectionLayer(BufferBuilder.RenderedBuffer pRenderedBuffer, VertexBuffer pVertexBuffer) {
      return this.closed ? CompletableFuture.completedFuture((Void)null) : CompletableFuture.runAsync(() -> {
         if (pVertexBuffer.isInvalid()) {
            pRenderedBuffer.release();
         } else {
            pVertexBuffer.bind();
            pVertexBuffer.upload(pRenderedBuffer);
            VertexBuffer.unbind();
         }

      }, this.toUpload::add);
   }

   private void clearBatchQueue() {
      while(!this.toBatchHighPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.toBatchHighPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            sectionrenderdispatcher$rendersection$compiletask.cancel();
         }
      }

      while(!this.toBatchLowPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask1 = this.toBatchLowPriority.poll();
         if (sectionrenderdispatcher$rendersection$compiletask1 != null) {
            sectionrenderdispatcher$rendersection$compiletask1.cancel();
         }
      }

      this.toBatchCount = 0;
   }

   public boolean isQueueEmpty() {
      return this.toBatchCount == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.closed = true;
      this.clearBatchQueue();
      this.uploadAllPendingUploads();
   }

   public void pauseChunkUpdates() {
      long i = System.currentTimeMillis();
      if (this.listPausedBuilders.size() <= 0) {
         while(this.listPausedBuilders.size() != this.countRenderBuilders) {
            this.uploadAllPendingUploads();
            SectionBufferBuilderPack sectionbufferbuilderpack = this.bufferPool.acquire();
            if (sectionbufferbuilderpack != null) {
               this.listPausedBuilders.add(sectionbufferbuilderpack);
            }

            if (System.currentTimeMillis() > i + 1000L) {
               break;
            }
         }

      }
   }

   public void resumeChunkUpdates() {
      for(SectionBufferBuilderPack sectionbufferbuilderpack : this.listPausedBuilders) {
         this.bufferPool.release(sectionbufferbuilderpack);
      }

      this.listPausedBuilders.clear();
   }

   public boolean updateChunkNow(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
      this.rebuildSectionSync(renderChunk, regionCacheIn);
      return true;
   }

   public boolean updateChunkLater(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
      if (this.bufferPool.isEmpty()) {
         return false;
      } else {
         renderChunk.rebuildSectionAsync(this, regionCacheIn);
         return true;
      }
   }

   public boolean updateTransparencyLater(SectionRenderDispatcher.RenderSection renderChunk) {
      return this.bufferPool.isEmpty() ? false : renderChunk.resortTransparency(RenderTypes.TRANSLUCENT, this);
   }

   public void addUploadTask(Runnable r) {
      if (r != null) {
         this.toUpload.add(r);
      }
   }

   public static class CompiledSection {
      public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
         public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_) {
            return false;
         }

         public void setAnimatedSprites(RenderType layer, BitSet animatedSprites) {
            throw new UnsupportedOperationException();
         }
      };
      final Set<RenderType> hasBlocks = new ChunkLayerSet();
      final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
      VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      BufferBuilder.SortState transparencyState;
      private BitSet[] animatedSprites = new BitSet[RenderType.CHUNK_RENDER_TYPES.length];

      public boolean hasNoRenderableLayers() {
         return this.hasBlocks.isEmpty();
      }

      public boolean isEmpty(RenderType pRenderType) {
         return !this.hasBlocks.contains(pRenderType);
      }

      public List<BlockEntity> getRenderableBlockEntities() {
         return this.renderableBlockEntities;
      }

      public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
         return this.visibilitySet.visibilityBetween(pFace1, pFace2);
      }

      public BitSet getAnimatedSprites(RenderType layer) {
         return this.animatedSprites[layer.ordinal()];
      }

      public void setAnimatedSprites(BitSet[] animatedSprites) {
         this.animatedSprites = animatedSprites;
      }

      public boolean isLayerUsed(RenderType renderTypeIn) {
         return this.hasBlocks.contains(renderTypeIn);
      }

      public void setLayerUsed(RenderType renderTypeIn) {
         this.hasBlocks.add(renderTypeIn);
      }

      public boolean hasTerrainBlockEntities() {
         return !this.hasNoRenderableLayers() || !this.getRenderableBlockEntities().isEmpty();
      }

      public Set<RenderType> getLayersUsed() {
         return this.hasBlocks;
      }
   }

   public class RenderSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
      final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
      @Nullable
      private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
      @Nullable
      private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
      private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
      private final ChunkLayerMap<VertexBuffer> buffers = new ChunkLayerMap<>((renderType) -> {
         return new VertexBuffer(VertexBuffer.Usage.STATIC);
      });
      private AABB bb;
      private boolean dirty = true;
      final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], (posArrIn) -> {
         for(int i = 0; i < posArrIn.length; ++i) {
            posArrIn[i] = new BlockPos.MutableBlockPos();
         }

      });
      private boolean playerChanged;
      private final boolean isMipmaps = Config.isMipmaps();
      private boolean playerUpdate = false;
      private boolean needsBackgroundPriorityUpdate;
      private boolean renderRegions = Config.isRenderRegions();
      public int regionX;
      public int regionZ;
      public int regionDX;
      public int regionDY;
      public int regionDZ;
      private final SectionRenderDispatcher.RenderSection[] renderChunksOfset16 = new SectionRenderDispatcher.RenderSection[6];
      private boolean renderChunksOffset16Updated = false;
      private LevelChunk chunk;
      private SectionRenderDispatcher.RenderSection[] renderChunkNeighbours = new SectionRenderDispatcher.RenderSection[Direction.VALUES.length];
      private SectionRenderDispatcher.RenderSection[] renderChunkNeighboursValid = new SectionRenderDispatcher.RenderSection[Direction.VALUES.length];
      private boolean renderChunkNeighboursUpated = false;
      private SectionOcclusionGraph.Node renderInfo = new SectionOcclusionGraph.Node(this, (Direction)null, 0);
      public AabbFrame boundingBoxParent;
      private SectionPos sectionPosition;

      public RenderSection(int pIndex, int pOriginX, int pOriginY, int pOriginZ) {
         this.index = pIndex;
         this.setOrigin(pOriginX, pOriginY, pOriginZ);
      }

      private boolean doesChunkExistAt(BlockPos pPos) {
         return SectionRenderDispatcher.this.level.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()), ChunkStatus.FULL, false) != null;
      }

      public boolean hasAllNeighbors() {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return (true);
         }
         int i = 24;
         return !(this.getDistToPlayerSqr() > 576.0D) ? true : this.doesChunkExistAt(this.origin);
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public VertexBuffer getBuffer(RenderType pRenderType) {
         return this.buffers.get(pRenderType);
      }

      public void setOrigin(int pX, int pY, int pZ) {
         this.reset();
         this.origin.set(pX, pY, pZ);
         this.sectionPosition = SectionPos.of(this.origin);
         if (this.renderRegions) {
            int i = 8;
            this.regionX = pX >> i << i;
            this.regionZ = pZ >> i << i;
            this.regionDX = pX - this.regionX;
            this.regionDY = pY;
            this.regionDZ = pZ - this.regionZ;
         }

         this.bb = new AABB((double)pX, (double)pY, (double)pZ, (double)(pX + 16), (double)(pY + 16), (double)(pZ + 16));

         for(Direction direction : Direction.VALUES) {
            this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
         }

         this.renderChunksOffset16Updated = false;
         this.renderChunkNeighboursUpated = false;

         for(int j = 0; j < this.renderChunkNeighbours.length; ++j) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.renderChunkNeighbours[j];
            if (sectionrenderdispatcher$rendersection != null) {
               sectionrenderdispatcher$rendersection.renderChunkNeighboursUpated = false;
            }
         }

         this.chunk = null;
         this.boundingBoxParent = null;
      }

      protected double getDistToPlayerSqr() {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         double d0 = this.bb.minX + 8.0D - camera.getPosition().x;
         double d1 = this.bb.minY + 8.0D - camera.getPosition().y;
         double d2 = this.bb.minZ + 8.0D - camera.getPosition().z;
         return d0 * d0 + d1 * d1 + d2 * d2;
      }

      void beginLayer(BufferBuilder pBufferBuilder) {
         pBufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
      }

      public SectionRenderDispatcher.CompiledSection getCompiled() {
         return this.compiled.get();
      }

      private void reset() {
         this.cancelTasks();
         this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
         this.dirty = true;
      }

      public void releaseBuffers() {
         this.reset();
         this.buffers.values().forEach(VertexBuffer::close);
      }

      public BlockPos getOrigin() {
         return this.origin;
      }

      public void setDirty(boolean pPlayerChanged) {
         boolean flag = this.dirty;
         this.dirty = true;
         this.playerChanged = pPlayerChanged | (flag && this.playerChanged);
         if (this.isWorldPlayerUpdate()) {
            this.playerUpdate = true;
         }

         if (!flag) {
            SectionRenderDispatcher.this.renderer.onChunkRenderNeedsUpdate(this);
         }

      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
         this.playerUpdate = false;
         this.needsBackgroundPriorityUpdate = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public BlockPos getRelativeOrigin(Direction pDirection) {
         return this.relativeOrigins[pDirection.ordinal()];
      }

      public boolean resortTransparency(RenderType pRenderType, SectionRenderDispatcher pSectionRenderDispatcher) {
         SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = this.getCompiled();
         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
         }

         if (!sectionrenderdispatcher$compiledsection.hasBlocks.contains(pRenderType)) {
            return false;
         } else {
            if (SectionRenderDispatcher.FORGE) {
               this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(new ChunkPos(this.getOrigin()), this.getDistToPlayerSqr(), sectionrenderdispatcher$compiledsection);
            } else {
               this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getDistToPlayerSqr(), sectionrenderdispatcher$compiledsection);
            }

            pSectionRenderDispatcher.schedule(this.lastResortTransparencyTask);
            return true;
         }
      }

      protected boolean cancelTasks() {
         boolean flag = false;
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            flag = true;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }

         return flag;
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
         boolean flag = this.cancelTasks();
         BlockPos blockpos = this.origin.immutable();
         int i = 1;
         RenderChunkRegion renderchunkregion = null;
         boolean flag1 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;
         if (flag1 && flag) {
            this.initialCompilationCancelCount.incrementAndGet();
         }

         ChunkPos chunkpos = SectionRenderDispatcher.FORGE ? new ChunkPos(this.getOrigin()) : null;
         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(chunkpos, this.getDistToPlayerSqr(), renderchunkregion, !flag1 || this.initialCompilationCancelCount.get() > 2);
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
         pSectionRenderDispatcher.schedule(sectionrenderdispatcher$rendersection$compiletask);
      }

      void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
         Set<BlockEntity> set = Sets.newHashSet(pBlockEntities);
         Set<BlockEntity> set1;
         synchronized(this.globalBlockEntities) {
            set1 = Sets.newHashSet(this.globalBlockEntities);
            set.removeAll(this.globalBlockEntities);
            set1.removeAll(pBlockEntities);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(pBlockEntities);
         }

         SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
      }

      public void compileSync(RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
         sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      public boolean isAxisAlignedWith(int pX, int pY, int pZ) {
         BlockPos blockpos = this.getOrigin();
         return pX == SectionPos.blockToSectionCoord(blockpos.getX()) || pZ == SectionPos.blockToSectionCoord(blockpos.getZ()) || pY == SectionPos.blockToSectionCoord(blockpos.getY());
      }

      private boolean isWorldPlayerUpdate() {
         if (SectionRenderDispatcher.this.level instanceof ClientLevel) {
            ClientLevel clientlevel = SectionRenderDispatcher.this.level;
            return clientlevel.isPlayerUpdate();
         } else {
            return false;
         }
      }

      public boolean isPlayerUpdate() {
         return this.playerUpdate;
      }

      public void setNeedsBackgroundPriorityUpdate(boolean needsBackgroundPriorityUpdate) {
         this.needsBackgroundPriorityUpdate = needsBackgroundPriorityUpdate;
      }

      public boolean needsBackgroundPriorityUpdate() {
         return this.needsBackgroundPriorityUpdate;
      }

      private Iterable<RenderType> getBlockRenderLayers(BakedModel model, BlockState blockState, BlockPos blockPos, RandomSource randomsource, ModelData modelData, SingleIterable<RenderType> singleLayer) {
         if (SectionRenderDispatcher.FORGE) {
            randomsource.setSeed(blockState.getSeed(blockPos));
            return model.getRenderTypes(blockState, randomsource, modelData);
         } else {
            singleLayer.setValue(ItemBlockRenderTypes.getChunkRenderType(blockState));
            return singleLayer;
         }
      }

      private RenderType fixBlockLayer(BlockGetter worldReader, BlockState blockState, BlockPos blockPos, RenderType layer) {
         if (CustomBlockLayers.isActive()) {
            RenderType rendertype = CustomBlockLayers.getRenderLayer(worldReader, blockState, blockPos);
            if (rendertype != null) {
               return rendertype;
            }
         }

         if (this.isMipmaps) {
            if (layer == RenderTypes.CUTOUT) {
               Block block = blockState.getBlock();
               if (block instanceof RedStoneWireBlock) {
                  return layer;
               }

               if (block instanceof CactusBlock) {
                  return layer;
               }

               return RenderTypes.CUTOUT_MIPPED;
            }
         } else if (layer == RenderTypes.CUTOUT_MIPPED) {
            return RenderTypes.CUTOUT;
         }

         return layer;
      }

      private void postRenderOverlays(SectionBufferBuilderPack regionRenderCacheBuilder, Set<RenderType> renderTypes) {
         this.postRenderOverlay(RenderTypes.CUTOUT, regionRenderCacheBuilder, renderTypes);
         this.postRenderOverlay(RenderTypes.CUTOUT_MIPPED, regionRenderCacheBuilder, renderTypes);
         this.postRenderOverlay(RenderTypes.TRANSLUCENT, regionRenderCacheBuilder, renderTypes);
      }

      private void postRenderOverlay(RenderType layer, SectionBufferBuilderPack regionRenderCacheBuilder, Set<RenderType> renderTypes) {
         BufferBuilder bufferbuilder = regionRenderCacheBuilder.builder(layer);
         if (bufferbuilder.building()) {
            renderTypes.add(layer);
         }

      }

      private ChunkCacheOF makeChunkCacheOF(BlockPos posIn) {
         BlockPos blockpos = posIn.offset(-1, -1, -1);
         BlockPos blockpos1 = posIn.offset(16, 16, 16);
         RenderRegionCache renderregioncache = new RenderRegionCache();
         RenderChunkRegion renderchunkregion = renderregioncache.createRegion(SectionRenderDispatcher.this.level, blockpos, blockpos1, 1, false);
         return new ChunkCacheOF(renderchunkregion, blockpos, blockpos1, 1);
      }

      public SectionRenderDispatcher.RenderSection getRenderChunkOffset16(ViewArea viewFrustum, Direction facing) {
         if (!this.renderChunksOffset16Updated) {
            for(int i = 0; i < Direction.VALUES.length; ++i) {
               Direction direction = Direction.VALUES[i];
               BlockPos blockpos = this.getRelativeOrigin(direction);
               this.renderChunksOfset16[i] = viewFrustum.getRenderSectionAt(blockpos);
            }

            this.renderChunksOffset16Updated = true;
         }

         return this.renderChunksOfset16[facing.ordinal()];
      }

      public LevelChunk getChunk() {
         return this.getChunk(this.origin);
      }

      private LevelChunk getChunk(BlockPos posIn) {
         LevelChunk levelchunk = this.chunk;
         if (levelchunk != null && ChunkUtils.isLoaded(levelchunk)) {
            return levelchunk;
         } else {
            levelchunk = SectionRenderDispatcher.this.level.getChunkAt(posIn);
            this.chunk = levelchunk;
            return levelchunk;
         }
      }

      public boolean isChunkRegionEmpty() {
         return this.isChunkRegionEmpty(this.origin);
      }

      private boolean isChunkRegionEmpty(BlockPos posIn) {
         int i = posIn.getY();
         int j = i + 15;
         return this.getChunk(posIn).isYSpaceEmpty(i, j);
      }

      public void setRenderChunkNeighbour(Direction facing, SectionRenderDispatcher.RenderSection neighbour) {
         this.renderChunkNeighbours[facing.ordinal()] = neighbour;
         this.renderChunkNeighboursValid[facing.ordinal()] = neighbour;
      }

      public SectionRenderDispatcher.RenderSection getRenderChunkNeighbour(Direction facing) {
         if (!this.renderChunkNeighboursUpated) {
            this.updateRenderChunkNeighboursValid();
         }

         return this.renderChunkNeighboursValid[facing.ordinal()];
      }

      public SectionOcclusionGraph.Node getRenderInfo() {
         return this.renderInfo;
      }

      public SectionOcclusionGraph.Node getRenderInfo(Direction dirIn, int counterIn) {
         this.renderInfo.initialize(dirIn, counterIn);
         return this.renderInfo;
      }

      private void updateRenderChunkNeighboursValid() {
         int i = this.getOrigin().getX();
         int j = this.getOrigin().getZ();
         int k = Direction.NORTH.ordinal();
         int l = Direction.SOUTH.ordinal();
         int i1 = Direction.WEST.ordinal();
         int j1 = Direction.EAST.ordinal();
         this.renderChunkNeighboursValid[k] = this.renderChunkNeighbours[k].getOrigin().getZ() == j - 16 ? this.renderChunkNeighbours[k] : null;
         this.renderChunkNeighboursValid[l] = this.renderChunkNeighbours[l].getOrigin().getZ() == j + 16 ? this.renderChunkNeighbours[l] : null;
         this.renderChunkNeighboursValid[i1] = this.renderChunkNeighbours[i1].getOrigin().getX() == i - 16 ? this.renderChunkNeighbours[i1] : null;
         this.renderChunkNeighboursValid[j1] = this.renderChunkNeighbours[j1].getOrigin().getX() == i + 16 ? this.renderChunkNeighbours[j1] : null;
         this.renderChunkNeighboursUpated = true;
      }

      public boolean isBoundingBoxInFrustum(ICamera camera, int frameCount) {
         return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(camera, frameCount) ? true : camera.isBoundingBoxInFrustum(this.bb);
      }

      public AabbFrame getBoundingBoxParent() {
         if (this.boundingBoxParent == null) {
            BlockPos blockpos = this.getOrigin();
            int i = blockpos.getX();
            int j = blockpos.getY();
            int k = blockpos.getZ();
            int l = 5;
            int i1 = i >> l << l;
            int j1 = j >> l << l;
            int k1 = k >> l << l;
            if (i1 != i || j1 != j || k1 != k) {
               AabbFrame aabbframe = SectionRenderDispatcher.this.renderer.getRenderChunk(new BlockPos(i1, j1, k1)).getBoundingBoxParent();
               if (aabbframe != null && aabbframe.minX == (double)i1 && aabbframe.minY == (double)j1 && aabbframe.minZ == (double)k1) {
                  this.boundingBoxParent = aabbframe;
               }
            }

            if (this.boundingBoxParent == null) {
               int l1 = 1 << l;
               this.boundingBoxParent = new AabbFrame((double)i1, (double)j1, (double)k1, (double)(i1 + l1), (double)(j1 + l1), (double)(k1 + l1));
            }
         }

         return this.boundingBoxParent;
      }

      public ClientLevel getWorld() {
         return SectionRenderDispatcher.this.level;
      }

      public SectionPos getSectionPosition() {
         return this.sectionPosition;
      }

      public String toString() {
         return "pos: " + this.getOrigin();
      }

      abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask> {
         protected final double distAtCreation;
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final boolean isHighPriority;
         protected Map<BlockPos, ModelData> modelData;

         public CompileTask(double pDistAtCreation, boolean pIsHighPriority) {
            this((ChunkPos)null, pDistAtCreation, pIsHighPriority);
         }

         public CompileTask(ChunkPos pos, double distanceSqIn, boolean highPriorityIn) {
            this.distAtCreation = distanceSqIn;
            this.isHighPriority = highPriorityIn;
            if (pos == null) {
               this.modelData = Collections.emptyMap();
            } else {
               this.modelData = Minecraft.getInstance().level.getModelDataManager().getAt(pos);
            }

         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

         public abstract void cancel();

         protected abstract String name();

         public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask pOther) {
            return Doubles.compare(this.distAtCreation, pOther.distAtCreation);
         }

         public ModelData getModelData(BlockPos pos) {
            return this.modelData.getOrDefault(pos, ModelData.EMPTY);
         }
      }

      class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         @Nullable
         protected RenderChunkRegion region;

         public RebuildTask(@Nullable double pDistAtCreation, RenderChunkRegion pRegion, boolean pIsHighPriority) {
            this((ChunkPos)null, pDistAtCreation, pRegion, pIsHighPriority);
         }

         public RebuildTask(ChunkPos pos, @Nullable double distanceSqIn, RenderChunkRegion renderCacheIn, boolean highPriorityIn) {
            super(pos, distanceSqIn, highPriorityIn);
            this.region = renderCacheIn;
         }

         protected String name() {
            return "rend_chk_rebuild";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.region = null;
               RenderSection.this.setDirty(false);
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
               float f = (float)vec3.x;
               float f1 = (float)vec3.y;
               float f2 = (float)vec3.z;
               SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults sectionrenderdispatcher$rendersection$rebuildtask$compileresults = this.compile(f, f1, f2, pSectionBufferBuilderPack);
               RenderSection.this.updateGlobalBlockEntities(sectionrenderdispatcher$rendersection$rebuildtask$compileresults.globalBlockEntities);
               if (this.isCancelled.get()) {
                  sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.values().forEach(BufferBuilder.RenderedBuffer::release);
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               } else {
                  SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = new SectionRenderDispatcher.CompiledSection();
                  sectionrenderdispatcher$compiledsection.visibilitySet = sectionrenderdispatcher$rendersection$rebuildtask$compileresults.visibilitySet;
                  sectionrenderdispatcher$compiledsection.renderableBlockEntities.addAll(sectionrenderdispatcher$rendersection$rebuildtask$compileresults.blockEntities);
                  sectionrenderdispatcher$compiledsection.transparencyState = sectionrenderdispatcher$rendersection$rebuildtask$compileresults.transparencyState;
                  sectionrenderdispatcher$compiledsection.setAnimatedSprites(sectionrenderdispatcher$rendersection$rebuildtask$compileresults.animatedSprites);
                  List<CompletableFuture<Void>> list = Lists.newArrayList();
                  sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.forEach((renderTypeIn, bufferIn) -> {
                     list.add(SectionRenderDispatcher.this.uploadSectionLayer(bufferIn, RenderSection.this.getBuffer(renderTypeIn)));
                     sectionrenderdispatcher$compiledsection.hasBlocks.add(renderTypeIn);
                  });
                  return Util.sequenceFailFast(list).handle((listIn, throwableIn) -> {
                     if (throwableIn != null && !(throwableIn instanceof CancellationException) && !(throwableIn instanceof InterruptedException)) {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                     }

                     if (this.isCancelled.get()) {
                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                     } else {
                        RenderSection.this.compiled.set(sectionrenderdispatcher$compiledsection);
                        RenderSection.this.initialCompilationCancelCount.set(0);
                        SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(RenderSection.this);
                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     }
                  });
               }
            }
         }

         private SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults compile(float pX, float pY, float pZ, SectionBufferBuilderPack pSectionBufferBuilderPack) {
            SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults sectionrenderdispatcher$rendersection$rebuildtask$compileresults = new SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults();
            int i = 1;
            BlockPos blockpos = RenderSection.this.origin.immutable();
            BlockPos blockpos1 = blockpos.offset(15, 15, 15);
            VisGraph visgraph = new VisGraph();
            this.region = null;
            PoseStack posestack = new PoseStack();
            if (!RenderSection.this.isChunkRegionEmpty(blockpos)) {
               ++SectionRenderDispatcher.renderChunksUpdated;
               ChunkCacheOF chunkcacheof = RenderSection.this.makeChunkCacheOF(blockpos);
               chunkcacheof.renderStart();
               SingleIterable<RenderType> singleiterable = new SingleIterable<>();
               boolean flag = Config.isShaders();
               boolean flag1 = flag && Shaders.useMidBlockAttrib;
               ModelBlockRenderer.enableCaching();
               Set<RenderType> set = new ReferenceArraySet<>(RenderType.chunkBufferLayers().size());
               RandomSource randomsource = RandomSource.create();
               BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();

               for(BlockPosM blockposm : (Iterable<BlockPosM>) BlockPosM.getAllInBoxMutable(blockpos, blockpos1)) {
                  BlockState blockstate = chunkcacheof.getBlockState(blockposm);
                  if (!blockstate.isAir()) {
                     if (blockstate.isSolidRender(chunkcacheof, blockposm)) {
                        visgraph.setOpaque(blockposm);
                     }

                     if (blockstate.hasBlockEntity()) {
                        BlockEntity blockentity = chunkcacheof.getBlockEntity(blockposm);
                        if (blockentity != null) {
                           this.handleBlockEntity(sectionrenderdispatcher$rendersection$rebuildtask$compileresults, blockentity);
                        }
                     }

                     FluidState fluidstate = blockstate.getFluidState();
                     if (!fluidstate.isEmpty()) {
                        RenderType rendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                        BufferBuilder bufferbuilder = pSectionBufferBuilderPack.builder(rendertype);
                        bufferbuilder.setBlockLayer(rendertype);
                        RenderEnv renderenv = bufferbuilder.getRenderEnv(blockstate, blockposm);
                        renderenv.setRegionRenderCacheBuilder(pSectionBufferBuilderPack);
                        chunkcacheof.setRenderEnv(renderenv);
                        if (set.add(rendertype)) {
                           RenderSection.this.beginLayer(bufferbuilder);
                        }

                        blockrenderdispatcher.renderLiquid(blockposm, chunkcacheof, bufferbuilder, blockstate, fluidstate);
                     }

                     if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                        BakedModel bakedmodel = blockrenderdispatcher.getBlockModel(blockstate);
                        ModelData modeldata = SectionRenderDispatcher.FORGE ? bakedmodel.getModelData(chunkcacheof, blockposm, blockstate, this.getModelData(blockposm)) : null;

                        for(RenderType rendertype1 : RenderSection.this.getBlockRenderLayers(bakedmodel, blockstate, blockposm, randomsource, modeldata, singleiterable)) {
                           RenderType rendertype2 = RenderSection.this.fixBlockLayer(chunkcacheof, blockstate, blockposm, rendertype1);
                           BufferBuilder bufferbuilder1 = pSectionBufferBuilderPack.builder(rendertype2);
                           bufferbuilder1.setBlockLayer(rendertype2);
                           RenderEnv renderenv1 = bufferbuilder1.getRenderEnv(blockstate, blockposm);
                           renderenv1.setRegionRenderCacheBuilder(pSectionBufferBuilderPack);
                           chunkcacheof.setRenderEnv(renderenv1);
                           if (set.add(rendertype2)) {
                              RenderSection.this.beginLayer(bufferbuilder1);
                           }

                           posestack.pushPose();
                           posestack.translate((float)RenderSection.this.regionDX + (float)(blockposm.getX() & 15), (float)RenderSection.this.regionDY + (float)(blockposm.getY() & 15), (float)RenderSection.this.regionDZ + (float)(blockposm.getZ() & 15));
                           if (flag1) {
                              bufferbuilder1.setMidBlock(0.5F + (float)RenderSection.this.regionDX + (float)(blockposm.getX() & 15), 0.5F + (float)RenderSection.this.regionDY + (float)(blockposm.getY() & 15), 0.5F + (float)RenderSection.this.regionDZ + (float)(blockposm.getZ() & 15));
                           }

                           blockrenderdispatcher.renderBatched(blockstate, blockposm, chunkcacheof, posestack, bufferbuilder1, true, randomsource, modeldata, rendertype1);
                           if (renderenv1.isOverlaysRendered()) {
                              RenderSection.this.postRenderOverlays(pSectionBufferBuilderPack, set);
                              renderenv1.setOverlaysRendered(false);
                           }

                           posestack.popPose();
                        }
                     }
                  }
               }

               if (set.contains(RenderType.translucent())) {
                  BufferBuilder bufferbuilder2 = pSectionBufferBuilderPack.builder(RenderType.translucent());
                  if (!bufferbuilder2.isCurrentBatchEmpty()) {
                     bufferbuilder2.setQuadSorting(VertexSorting.byDistance((float)RenderSection.this.regionDX + pX - (float)blockpos.getX(), (float)RenderSection.this.regionDY + pY - (float)blockpos.getY(), (float)RenderSection.this.regionDZ + pZ - (float)blockpos.getZ()));
                     sectionrenderdispatcher$rendersection$rebuildtask$compileresults.transparencyState = bufferbuilder2.getSortState();
                  }
               }

               for(RenderType rendertype3 : set) {
                  BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = pSectionBufferBuilderPack.builder(rendertype3).endOrDiscardIfEmpty();
                  if (bufferbuilder$renderedbuffer != null) {
                     sectionrenderdispatcher$rendersection$rebuildtask$compileresults.renderedLayers.put(rendertype3, bufferbuilder$renderedbuffer);
                  }
               }

               for(RenderType rendertype5 : SectionRenderDispatcher.BLOCK_RENDER_LAYERS) {
                  sectionrenderdispatcher$rendersection$rebuildtask$compileresults.setAnimatedSprites(rendertype5, (BitSet)null);
               }

               for(RenderType rendertype4 : set) {
                  if (Config.isShaders()) {
                     SVertexBuilder.calcNormalChunkLayer(pSectionBufferBuilderPack.builder(rendertype4));
                  }

                  BufferBuilder bufferbuilder3 = pSectionBufferBuilderPack.builder(rendertype4);
                  if (bufferbuilder3.animatedSprites != null && !bufferbuilder3.animatedSprites.isEmpty()) {
                     sectionrenderdispatcher$rendersection$rebuildtask$compileresults.setAnimatedSprites(rendertype4, (BitSet)bufferbuilder3.animatedSprites.clone());
                  }
               }

               chunkcacheof.renderFinish();
               ModelBlockRenderer.clearCache();
            }

            sectionrenderdispatcher$rendersection$rebuildtask$compileresults.visibilitySet = visgraph.resolve();
            return sectionrenderdispatcher$rendersection$rebuildtask$compileresults;
         }

         private <E extends BlockEntity> void handleBlockEntity(SectionRenderDispatcher.RenderSection.RebuildTask.CompileResults pCompileResults, E pBlockEntity) {
            BlockEntityRenderer<E> blockentityrenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(pBlockEntity);
            if (blockentityrenderer != null) {
               if (blockentityrenderer.shouldRenderOffScreen(pBlockEntity)) {
                  pCompileResults.globalBlockEntities.add(pBlockEntity);
               } else {
                  pCompileResults.blockEntities.add(pBlockEntity);
               }
            }

         }

         public void cancel() {
            this.region = null;
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }

         }

         static final class CompileResults {
            public final List<BlockEntity> globalBlockEntities = new ArrayList<>();
            public final List<BlockEntity> blockEntities = new ArrayList<>();
            public final Map<RenderType, BufferBuilder.RenderedBuffer> renderedLayers = new Reference2ObjectArrayMap<>();
            public VisibilitySet visibilitySet = new VisibilitySet();
            @Nullable
            public BufferBuilder.SortState transparencyState;
            public BitSet[] animatedSprites = new BitSet[RenderType.CHUNK_RENDER_TYPES.length];

            public void setAnimatedSprites(RenderType layer, BitSet animatedSprites) {
               this.animatedSprites[layer.ordinal()] = animatedSprites;
            }
         }
      }

      class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final SectionRenderDispatcher.CompiledSection compiledSection;

         public ResortTransparencyTask(double pDistAtCreation, SectionRenderDispatcher.CompiledSection pCompiledSection) {
            this((ChunkPos)null, pDistAtCreation, pCompiledSection);
         }

         public ResortTransparencyTask(ChunkPos pos, double distanceSqIn, SectionRenderDispatcher.CompiledSection compiledChunkIn) {
            super(pos, distanceSqIn, true);
            this.compiledSection = compiledChunkIn;
         }

         protected String name() {
            return "rend_chk_sort";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
               float f = (float)vec3.x;
               float f1 = (float)vec3.y;
               float f2 = (float)vec3.z;
               BufferBuilder.SortState bufferbuilder$sortstate = this.compiledSection.transparencyState;
               if (bufferbuilder$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                  BufferBuilder bufferbuilder = pSectionBufferBuilderPack.builder(RenderType.translucent());
                  bufferbuilder.setBlockLayer(RenderType.translucent());
                  RenderSection.this.beginLayer(bufferbuilder);
                  bufferbuilder.restoreSortState(bufferbuilder$sortstate);
                  bufferbuilder.setQuadSorting(VertexSorting.byDistance((float)RenderSection.this.regionDX + f - (float)RenderSection.this.origin.getX(), (float)RenderSection.this.regionDY + f1 - (float)RenderSection.this.origin.getY(), (float)RenderSection.this.regionDZ + f2 - (float)RenderSection.this.origin.getZ()));
                  this.compiledSection.transparencyState = bufferbuilder.getSortState();
                  BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = bufferbuilder.end();
                  if (this.isCancelled.get()) {
                     bufferbuilder$renderedbuffer.release();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = SectionRenderDispatcher.this.uploadSectionLayer(bufferbuilder$renderedbuffer, RenderSection.this.getBuffer(RenderType.translucent())).thenApply((voidIn) -> {
                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                     });
                     return completablefuture.handle((taskResultIn, throwableIn) -> {
                        if (throwableIn != null && !(throwableIn instanceof CancellationException) && !(throwableIn instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                        }

                        return this.isCancelled.get() ? SectionRenderDispatcher.SectionTaskResult.CANCELLED : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     });
                  }
               } else {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               }
            }
         }

         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }

   static enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }
}
