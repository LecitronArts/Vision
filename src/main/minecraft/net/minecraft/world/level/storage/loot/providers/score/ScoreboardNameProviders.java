package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class ScoreboardNameProviders {
   private static final Codec<ScoreboardNameProvider> TYPED_CODEC = BuiltInRegistries.LOOT_SCORE_PROVIDER_TYPE.byNameCodec().dispatch(ScoreboardNameProvider::getType, LootScoreProviderType::codec);
   public static final Codec<ScoreboardNameProvider> CODEC = ExtraCodecs.lazyInitializedCodec(() -> {
      return Codec.either(ContextScoreboardNameProvider.INLINE_CODEC, TYPED_CODEC).xmap((p_297850_) -> {
         return p_297850_.map(Function.identity(), Function.identity());
      }, (p_297604_) -> {
         Either either;
         if (p_297604_ instanceof ContextScoreboardNameProvider contextscoreboardnameprovider) {
            either = Either.left(contextscoreboardnameprovider);
         } else {
            either = Either.right(p_297604_);
         }

         return either;
      });
   });
   public static final LootScoreProviderType FIXED = register("fixed", FixedScoreboardNameProvider.CODEC);
   public static final LootScoreProviderType CONTEXT = register("context", ContextScoreboardNameProvider.CODEC);

   private static LootScoreProviderType register(String pName, Codec<? extends ScoreboardNameProvider> pCodec) {
      return Registry.register(BuiltInRegistries.LOOT_SCORE_PROVIDER_TYPE, new ResourceLocation(pName), new LootScoreProviderType(pCodec));
   }
}