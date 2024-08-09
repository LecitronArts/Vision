package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.scores.ScoreHolder;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
   public static final Codec<FixedScoreboardNameProvider> CODEC = RecordCodecBuilder.create((p_300953_) -> {
      return p_300953_.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name)).apply(p_300953_, FixedScoreboardNameProvider::new);
   });

   public static ScoreboardNameProvider forName(String pName) {
      return new FixedScoreboardNameProvider(pName);
   }

   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.FIXED;
   }

   public ScoreHolder getScoreHolder(LootContext pContext) {
      return ScoreHolder.forNameOnly(this.name);
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of();
   }
}