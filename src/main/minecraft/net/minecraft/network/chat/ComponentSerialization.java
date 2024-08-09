package net.minecraft.network.chat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public class ComponentSerialization {
   public static final Codec<Component> CODEC = ExtraCodecs.recursive("Component", ComponentSerialization::createCodec);
   public static final Codec<Component> FLAT_CODEC = ExtraCodecs.FLAT_JSON.flatXmap((p_309576_) -> {
      return CODEC.parse(JsonOps.INSTANCE, p_309576_);
   }, (p_310173_) -> {
      return CODEC.encodeStart(JsonOps.INSTANCE, p_310173_);
   });

   private static MutableComponent createFromList(List<Component> p_312708_) {
      MutableComponent mutablecomponent = p_312708_.get(0).copy();

      for(int i = 1; i < p_312708_.size(); ++i) {
         mutablecomponent.append(p_312708_.get(i));
      }

      return mutablecomponent;
   }

   public static <T extends StringRepresentable, E> MapCodec<E> createLegacyComponentMatcher(T[] pTypes, Function<T, MapCodec<? extends E>> pCodecGetter, Function<E, T> pTypeGetter, String pTypeFieldName) {
      MapCodec<E> mapcodec = new ComponentSerialization.FuzzyCodec<>(Stream.<T>of(pTypes).map(pCodecGetter).toList(), (p_312251_) -> {
         return pCodecGetter.apply(pTypeGetter.apply(p_312251_));
      });
      Codec<T> codec = StringRepresentable.fromValues(() -> {
         return pTypes;
      });
      MapCodec<E> mapcodec1 = codec.dispatchMap(pTypeFieldName, pTypeGetter, (p_312465_) -> {
         return pCodecGetter.apply(p_312465_).codec();
      });
      MapCodec<E> mapcodec2 = new ComponentSerialization.StrictEither<>(pTypeFieldName, mapcodec1, mapcodec);
      return ExtraCodecs.orCompressed(mapcodec2, mapcodec1);
   }

   private static Codec<Component> createCodec(Codec<Component> p_310353_) {
      ComponentContents.Type<?>[] type = new ComponentContents.Type[]{PlainTextContents.TYPE, TranslatableContents.TYPE, KeybindContents.TYPE, ScoreContents.TYPE, SelectorContents.TYPE, NbtContents.TYPE};
      MapCodec<ComponentContents> mapcodec = createLegacyComponentMatcher(type, ComponentContents.Type::codec, ComponentContents::type, "type");
      Codec<Component> codec = RecordCodecBuilder.create((p_311001_) -> {
         return p_311001_.group(mapcodec.forGetter(Component::getContents), ExtraCodecs.strictOptionalField(ExtraCodecs.nonEmptyList(p_310353_.listOf()), "extra", List.of()).forGetter(Component::getSiblings), Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)).apply(p_311001_, MutableComponent::new);
      });
      return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(p_310353_.listOf())), codec).xmap((p_312362_) -> {
         return p_312362_.map((p_310114_) -> {
            return p_310114_.map(Component::literal, ComponentSerialization::createFromList);
         }, (p_310523_) -> {
            return p_310523_;
         });
      }, (p_312558_) -> {
         String s = p_312558_.tryCollapseToString();
         return s != null ? Either.left(Either.left(s)) : Either.right(p_312558_);
      });
   }

   static class FuzzyCodec<T> extends MapCodec<T> {
      private final List<MapCodec<? extends T>> codecs;
      private final Function<T, MapEncoder<? extends T>> encoderGetter;

      public FuzzyCodec(List<MapCodec<? extends T>> pCodecs, Function<T, MapEncoder<? extends T>> pEncoderGetter) {
         this.codecs = pCodecs;
         this.encoderGetter = pEncoderGetter;
      }

      public <S> DataResult<T> decode(DynamicOps<S> pOps, MapLike<S> pInput) {
         for(MapDecoder<? extends T> mapdecoder : this.codecs) {
            DataResult<? extends T> dataresult = mapdecoder.decode(pOps, pInput);
            if (dataresult.result().isPresent()) {
               return (DataResult<T>)dataresult;
            }
         }

         return DataResult.error(() -> {
            return "No matching codec found";
         });
      }

      public <S> RecordBuilder<S> encode(T pInput, DynamicOps<S> pOps, RecordBuilder<S> pPrefix) {
         MapEncoder<T> mapencoder = (MapEncoder<T>)this.encoderGetter.apply(pInput);
         return mapencoder.encode(pInput, pOps, pPrefix);
      }

      public <S> Stream<S> keys(DynamicOps<S> pOps) {
         return this.codecs.stream().flatMap((p_310919_) -> {
            return p_310919_.keys(pOps);
         }).distinct();
      }

      public String toString() {
         return "FuzzyCodec[" + this.codecs + "]";
      }
   }

   static class StrictEither<T> extends MapCodec<T> {
      private final String typeFieldName;
      private final MapCodec<T> typed;
      private final MapCodec<T> fuzzy;

      public StrictEither(String pTypeFieldName, MapCodec<T> pTyped, MapCodec<T> pFuzzy) {
         this.typeFieldName = pTypeFieldName;
         this.typed = pTyped;
         this.fuzzy = pFuzzy;
      }

      public <O> DataResult<T> decode(DynamicOps<O> pOps, MapLike<O> pInput) {
         return pInput.get(this.typeFieldName) != null ? this.typed.decode(pOps, pInput) : this.fuzzy.decode(pOps, pInput);
      }

      public <O> RecordBuilder<O> encode(T pInput, DynamicOps<O> pOps, RecordBuilder<O> pPrefix) {
         return this.fuzzy.encode(pInput, pOps, pPrefix);
      }

      public <T1> Stream<T1> keys(DynamicOps<T1> pOps) {
         return Stream.concat(this.typed.keys(pOps), this.fuzzy.keys(pOps)).distinct();
      }
   }
}