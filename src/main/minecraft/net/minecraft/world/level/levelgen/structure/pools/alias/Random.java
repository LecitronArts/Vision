package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

record Random(ResourceKey<StructureTemplatePool> alias, SimpleWeightedRandomList<ResourceKey<StructureTemplatePool>> targets) implements PoolAliasBinding {
   static Codec<Random> CODEC = RecordCodecBuilder.create((p_311839_) -> {
      return p_311839_.group(ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("alias").forGetter(Random::alias), SimpleWeightedRandomList.wrappedCodec(ResourceKey.codec(Registries.TEMPLATE_POOL)).fieldOf("targets").forGetter(Random::targets)).apply(p_311839_, Random::new);
   });

   public void forEachResolved(RandomSource p_312605_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_311412_) {
      this.targets.getRandom(p_312605_).ifPresent((p_310704_) -> {
         p_311412_.accept(this.alias, p_310704_.getData());
      });
   }

   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return this.targets.unwrap().stream().map(WeightedEntry.Wrapper::getData);
   }

   public Codec<Random> codec() {
      return CODEC;
   }
}