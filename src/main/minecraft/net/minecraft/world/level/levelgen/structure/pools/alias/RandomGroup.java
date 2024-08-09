package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

record RandomGroup(SimpleWeightedRandomList<List<PoolAliasBinding>> groups) implements PoolAliasBinding {
   static Codec<RandomGroup> CODEC = RecordCodecBuilder.create((p_311954_) -> {
      return p_311954_.group(SimpleWeightedRandomList.wrappedCodec(Codec.list(PoolAliasBinding.CODEC)).fieldOf("groups").forGetter(RandomGroup::groups)).apply(p_311954_, RandomGroup::new);
   });

   public void forEachResolved(RandomSource p_309696_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_312789_) {
      this.groups.getRandom(p_309696_).ifPresent((p_311593_) -> {
         p_311593_.getData().forEach((p_313096_) -> {
            p_313096_.forEachResolved(p_309696_, p_312789_);
         });
      });
   }

   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return this.groups.unwrap().stream().flatMap((p_311806_) -> {
         return p_311806_.getData().stream();
      }).flatMap(PoolAliasBinding::allTargets);
   }

   public Codec<RandomGroup> codec() {
      return CODEC;
   }
}