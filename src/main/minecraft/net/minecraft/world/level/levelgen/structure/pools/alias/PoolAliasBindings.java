package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class PoolAliasBindings {
   public static Codec<? extends PoolAliasBinding> bootstrap(Registry<Codec<? extends PoolAliasBinding>> pRegistry) {
      Registry.register(pRegistry, "random", Random.CODEC);
      Registry.register(pRegistry, "random_group", RandomGroup.CODEC);
      return Registry.register(pRegistry, "direct", Direct.CODEC);
   }

   public static void registerTargetsAsPools(BootstapContext<StructureTemplatePool> pContext, Holder<StructureTemplatePool> pPool, List<PoolAliasBinding> pBindings) {
      pBindings.stream().flatMap(PoolAliasBinding::allTargets).map((p_312426_) -> {
         return p_312426_.location().getPath();
      }).forEach((p_310643_) -> {
         Pools.register(pContext, p_310643_, new StructureTemplatePool(pPool, List.of(Pair.of(StructurePoolElement.single(p_310643_), 1)), StructureTemplatePool.Projection.RIGID));
      });
   }
}