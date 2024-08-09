package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

record Direct(ResourceKey<StructureTemplatePool> alias, ResourceKey<StructureTemplatePool> target) implements PoolAliasBinding {
   static Codec<Direct> CODEC = RecordCodecBuilder.create((p_311220_) -> {
      return p_311220_.group(ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("alias").forGetter(Direct::alias), ResourceKey.codec(Registries.TEMPLATE_POOL).fieldOf("target").forGetter(Direct::target)).apply(p_311220_, Direct::new);
   });

   public void forEachResolved(RandomSource p_312348_, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> p_310565_) {
      p_310565_.accept(this.alias, this.target);
   }

   public Stream<ResourceKey<StructureTemplatePool>> allTargets() {
      return Stream.of(this.target);
   }

   public Codec<Direct> codec() {
      return CODEC;
   }
}