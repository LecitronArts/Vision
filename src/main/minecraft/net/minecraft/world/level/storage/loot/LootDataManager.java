package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootDataManager implements PreparableReloadListener, LootDataResolver {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = (new GsonBuilder()).create();
   public static final LootDataId<LootTable> EMPTY_LOOT_TABLE_KEY = new LootDataId<>(LootDataType.TABLE, BuiltInLootTables.EMPTY);
   private Map<LootDataId<?>, ?> elements = Map.of();
   private Multimap<LootDataType<?>, ResourceLocation> typeKeys = ImmutableMultimap.of();

   public final CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier pPreparationBarrier, ResourceManager pResourceManager, ProfilerFiller pPreparationsProfiler, ProfilerFiller pReloadProfiler, Executor pBackgroundExecutor, Executor pGameExecutor) {
      Map<LootDataType<?>, Map<ResourceLocation, ?>> map = new HashMap<>();
      CompletableFuture<?>[] completablefuture = LootDataType.values().map((p_279242_) -> {
         return scheduleElementParse(p_279242_, pResourceManager, pBackgroundExecutor, map);
      }).toArray((p_279126_) -> {
         return new CompletableFuture[p_279126_];
      });
      return CompletableFuture.allOf(completablefuture).thenCompose(pPreparationBarrier::wait).thenAcceptAsync((p_279096_) -> {
         this.apply(map);
      }, pGameExecutor);
   }

   private static <T> CompletableFuture<?> scheduleElementParse(LootDataType<T> pLootDataType, ResourceManager pResourceManager, Executor pBackgroundExecutor, Map<LootDataType<?>, Map<ResourceLocation, ?>> pElementCollector) {
      Map<ResourceLocation, T> map = new HashMap<>();
      pElementCollector.put(pLootDataType, map);
      return CompletableFuture.runAsync(() -> {
         Map<ResourceLocation, JsonElement> map1 = new HashMap<>();
         SimpleJsonResourceReloadListener.scanDirectory(pResourceManager, pLootDataType.directory(), GSON, map1);
         map1.forEach((p_279416_, p_279151_) -> {
            pLootDataType.deserialize(p_279416_, p_279151_).ifPresent((p_279295_) -> {
               map.put(p_279416_, p_279295_);
            });
         });
      }, pBackgroundExecutor);
   }

   private void apply(Map<LootDataType<?>, Map<ResourceLocation, ?>> pCollectedElements) {
      Object object = pCollectedElements.get(LootDataType.TABLE).remove(BuiltInLootTables.EMPTY);
      if (object != null) {
         LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", (Object)BuiltInLootTables.EMPTY);
      }

      ImmutableMap.Builder<LootDataId<?>, Object> builder = ImmutableMap.builder();
      ImmutableMultimap.Builder<LootDataType<?>, ResourceLocation> builder1 = ImmutableMultimap.builder();
      pCollectedElements.forEach((p_279449_, p_279262_) -> {
         p_279262_.forEach((p_279130_, p_279313_) -> {
            builder.put(new LootDataId(p_279449_, p_279130_), p_279313_);
            builder1.put(p_279449_, p_279130_);
         });
      });
      builder.put(EMPTY_LOOT_TABLE_KEY, LootTable.EMPTY);
      ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
      final Map<LootDataId<?>, ?> map = builder.build();
      ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, new LootDataResolver() {
         @Nullable
         public <T> T getElement(LootDataId<T> p_279194_) {
            return (T)map.get(p_279194_);
         }
      });
      map.forEach((p_279387_, p_279087_) -> {
         castAndValidate(validationcontext, p_279387_, p_279087_);
      });
      problemreporter$collector.get().forEach((p_279487_, p_279312_) -> {
         LOGGER.warn("Found loot table element validation problem in {}: {}", p_279487_, p_279312_);
      });
      this.elements = map;
      this.typeKeys = builder1.build();
   }

   private static <T> void castAndValidate(ValidationContext pContext, LootDataId<T> pId, Object pElement) {
      pId.type().runValidation(pContext, pId, (T)pElement);
   }

   @Nullable
   public <T> T getElement(LootDataId<T> pId) {
      return (T)this.elements.get(pId);
   }

   public Collection<ResourceLocation> getKeys(LootDataType<?> pType) {
      return this.typeKeys.get(pType);
   }
}