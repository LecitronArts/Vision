package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public class AdvancementProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;
   private final List<AdvancementSubProvider> subProviders;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public AdvancementProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries, List<AdvancementSubProvider> pSubProviders) {
      this.pathProvider = pOutput.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
      this.subProviders = pSubProviders;
      this.registries = pRegistries;
   }

   public CompletableFuture<?> run(CachedOutput pOutput) {
      return this.registries.thenCompose((p_255484_) -> {
         Set<ResourceLocation> set = new HashSet<>();
         List<CompletableFuture<?>> list = new ArrayList<>();
         Consumer<AdvancementHolder> consumer = (p_308487_) -> {
            if (!set.add(p_308487_.id())) {
               throw new IllegalStateException("Duplicate advancement " + p_308487_.id());
            } else {
               Path path = this.pathProvider.json(p_308487_.id());
               list.add(DataProvider.saveStable(pOutput, Advancement.CODEC, p_308487_.value(), path));
            }
         };

         for(AdvancementSubProvider advancementsubprovider : this.subProviders) {
            advancementsubprovider.generate(p_255484_, consumer);
         }

         return CompletableFuture.allOf(list.toArray((p_253393_) -> {
            return new CompletableFuture[p_253393_];
         }));
      });
   }

   public final String getName() {
      return "Advancements";
   }
}