package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
   public static final Codec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.create((p_297459_) -> {
      return p_297459_.group(NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n), NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)).apply(p_297459_, BinomialDistributionGenerator::new);
   });

   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   public int getInt(LootContext pLootContext) {
      int i = this.n.getInt(pLootContext);
      float f = this.p.getFloat(pLootContext);
      RandomSource randomsource = pLootContext.getRandom();
      int j = 0;

      for(int k = 0; k < i; ++k) {
         if (randomsource.nextFloat() < f) {
            ++j;
         }
      }

      return j;
   }

   public float getFloat(LootContext pLootContext) {
      return (float)this.getInt(pLootContext);
   }

   public static BinomialDistributionGenerator binomial(int pN, float pP) {
      return new BinomialDistributionGenerator(ConstantValue.exactly((float)pN), ConstantValue.exactly(pP));
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }
}