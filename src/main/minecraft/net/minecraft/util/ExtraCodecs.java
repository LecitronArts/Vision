package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExtraCodecs {
   public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
   public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
   public static final Codec<JsonElement> FLAT_JSON = Codec.STRING.flatXmap((p_308957_) -> {
      try {
         return DataResult.success(JsonParser.parseString(p_308957_));
      } catch (JsonParseException jsonparseexception) {
         return DataResult.error(jsonparseexception::getMessage);
      }
   }, (p_308960_) -> {
      try {
         return DataResult.success(GsonHelper.toStableString(p_308960_));
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(illegalargumentexception::getMessage);
      }
   });
   public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT.listOf().comapFlatMap((p_253502_) -> {
      return Util.fixedSize(p_253502_, 3).map((p_253489_) -> {
         return new Vector3f(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2));
      });
   }, (p_269787_) -> {
      return List.of(p_269787_.x(), p_269787_.y(), p_269787_.z());
   });
   public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT.listOf().comapFlatMap((p_269773_) -> {
      return Util.fixedSize(p_269773_, 4).map((p_269785_) -> {
         return new Quaternionf(p_269785_.get(0), p_269785_.get(1), p_269785_.get(2), p_269785_.get(3));
      });
   }, (p_269780_) -> {
      return List.of(p_269780_.x, p_269780_.y, p_269780_.z, p_269780_.w);
   });
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create((p_269774_) -> {
      return p_269774_.group(Codec.FLOAT.fieldOf("angle").forGetter((p_269776_) -> {
         return p_269776_.angle;
      }), VECTOR3F.fieldOf("axis").forGetter((p_269778_) -> {
         return new Vector3f(p_269778_.x, p_269778_.y, p_269778_.z);
      })).apply(p_269774_, AxisAngle4f::new);
   });
   public static final Codec<Quaternionf> QUATERNIONF = withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
   public static Codec<Matrix4f> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap((p_269788_) -> {
      return Util.fixedSize(p_269788_, 16).map((p_269777_) -> {
         Matrix4f matrix4f = new Matrix4f();

         for(int i = 0; i < p_269777_.size(); ++i) {
            matrix4f.setRowColumn(i >> 2, i & 3, p_269777_.get(i));
         }

         return matrix4f.determineProperties();
      });
   }, (p_269775_) -> {
      FloatList floatlist = new FloatArrayList(16);

      for(int i = 0; i < 16; ++i) {
         floatlist.add(p_269775_.getRowColumn(i >> 2, i & 3));
      }

      return floatlist;
   });
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, (p_275703_) -> {
      return "Value must be non-negative: " + p_275703_;
   });
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, (p_274847_) -> {
      return "Value must be positive: " + p_274847_;
   });
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, (p_274876_) -> {
      return "Value must be positive: " + p_274876_;
   });
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap((p_274857_) -> {
      try {
         return DataResult.success(Pattern.compile(p_274857_));
      } catch (PatternSyntaxException patternsyntaxexception) {
         return DataResult.error(() -> {
            return "Invalid regex pattern '" + p_274857_ + "': " + patternsyntaxexception.getMessage();
         });
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap((p_274852_) -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(p_274852_));
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(() -> {
            return "Malformed base64 string";
         });
      }
   }, (p_216180_) -> {
      return Base64.getEncoder().encodeToString(p_216180_);
   });
   public static final Codec<String> ESCAPED_STRING = Codec.STRING.comapFlatMap((p_296617_) -> {
      return DataResult.success(StringEscapeUtils.unescapeJava(p_296617_));
   }, StringEscapeUtils::escapeJava);
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING.comapFlatMap((p_216169_) -> {
      return p_216169_.startsWith("#") ? ResourceLocation.read(p_216169_.substring(1)).map((p_216182_) -> {
         return new ExtraCodecs.TagOrElementLocation(p_216182_, true);
      }) : ResourceLocation.read(p_216169_).map((p_216165_) -> {
         return new ExtraCodecs.TagOrElementLocation(p_216165_, false);
      });
   }, ExtraCodecs.TagOrElementLocation::decoratedId);
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = (p_216176_) -> {
      return p_216176_.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   };
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = (p_216178_) -> {
      return p_216178_.isPresent() ? Optional.of(p_216178_.getAsLong()) : Optional.empty();
   };
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM.xmap((p_253514_) -> {
      return BitSet.valueOf(p_253514_.toArray());
   }, (p_253493_) -> {
      return Arrays.stream(p_253493_.toLongArray());
   });
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create((p_253491_) -> {
      return p_253491_.group(Codec.STRING.fieldOf("name").forGetter(Property::name), Codec.STRING.fieldOf("value").forGetter(Property::value), Codec.STRING.optionalFieldOf("signature").forGetter((p_296611_) -> {
         return Optional.ofNullable(p_296611_.signature());
      })).apply(p_253491_, (p_253494_, p_253495_, p_253496_) -> {
         return new Property(p_253494_, p_253495_, p_253496_.orElse((String)null));
      });
   });
   @VisibleForTesting
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf()).xmap((p_253515_) -> {
      PropertyMap propertymap = new PropertyMap();
      p_253515_.ifLeft((p_253506_) -> {
         p_253506_.forEach((p_253500_, p_253501_) -> {
            for(String s : p_253501_) {
               propertymap.put(p_253500_, new Property(p_253500_, s));
            }

         });
      }).ifRight((p_296607_) -> {
         for(Property property : p_296607_) {
            propertymap.put(property.name(), property);
         }

      });
      return propertymap;
   }, (p_253504_) -> {
      return Either.right(p_253504_.values().stream().toList());
   });
   private static final MapCodec<GameProfile> GAME_PROFILE_WITHOUT_PROPERTIES = RecordCodecBuilder.mapCodec((p_296612_) -> {
      return p_296612_.group(UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(GameProfile::getId), Codec.STRING.fieldOf("name").forGetter(GameProfile::getName)).apply(p_296612_, GameProfile::new);
   });
   public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create((p_296608_) -> {
      return p_296608_.group(GAME_PROFILE_WITHOUT_PROPERTIES.forGetter(Function.identity()), PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)).apply(p_296608_, (p_253518_, p_253519_) -> {
         p_253519_.forEach((p_253511_, p_253512_) -> {
            p_253518_.getProperties().put(p_253511_, p_253512_);
         });
         return p_253518_;
      });
   });
   public static final Codec<String> NON_EMPTY_STRING = validate(Codec.STRING, (p_274858_) -> {
      return p_274858_.isEmpty() ? DataResult.error(() -> {
         return "Expected non-empty string";
      }) : DataResult.success(p_274858_);
   });
   public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap((p_284688_) -> {
      int[] aint = p_284688_.codePoints().toArray();
      return aint.length != 1 ? DataResult.error(() -> {
         return "Expected one codepoint, got: " + p_284688_;
      }) : DataResult.success(aint[0]);
   }, Character::toString);
   public static Codec<String> RESOURCE_PATH_CODEC = validate(Codec.STRING, (p_296613_) -> {
      return !ResourceLocation.isValidPath(p_296613_) ? DataResult.error(() -> {
         return "Invalid string to use as a resource path element: " + p_296613_;
      }) : DataResult.success(p_296613_);
   });

   public static <T> Codec<T> converter(DynamicOps<T> pOps) {
      return Codec.PASSTHROUGH.xmap((p_308959_) -> {
         return p_308959_.convert(pOps).getValue();
      }, (p_308962_) -> {
         return new Dynamic<>(pOps, p_308962_);
      });
   }

   public static <F, S> Codec<Either<F, S>> xor(Codec<F> pFirst, Codec<S> pSecond) {
      return new ExtraCodecs.XorCodec<>(pFirst, pSecond);
   }

   public static <P, I> Codec<I> intervalCodec(Codec<P> pCodec, String pMinFieldName, String pMaxFieldName, BiFunction<P, P, DataResult<I>> pFactory, Function<I, P> pMinGetter, Function<I, P> pMaxGetter) {
      Codec<I> codec = Codec.list(pCodec).comapFlatMap((p_184398_) -> {
         return Util.fixedSize(p_184398_, 2).flatMap((p_184445_) -> {
            P p = p_184445_.get(0);
            P p1 = p_184445_.get(1);
            return pFactory.apply(p, p1);
         });
      }, (p_184459_) -> {
         return ImmutableList.of(pMinGetter.apply(p_184459_), pMaxGetter.apply(p_184459_));
      });
      Codec<I> codec1 = RecordCodecBuilder.<Pair<P,P>>create((p_184360_) -> {
         return p_184360_.group(pCodec.fieldOf(pMinFieldName).forGetter(Pair::getFirst), pCodec.fieldOf(pMaxFieldName).forGetter(Pair::getSecond)).apply(p_184360_, Pair::of);
      }).comapFlatMap((p_184392_) -> {
         return pFactory.apply((P)p_184392_.getFirst(), (P)p_184392_.getSecond());
      }, (p_184449_) -> {
         return Pair.of(pMinGetter.apply(p_184449_), pMaxGetter.apply(p_184449_));
      });
      Codec<I> codec2 = withAlternative(codec, codec1);
      return Codec.either(pCodec, codec2).comapFlatMap((p_184389_) -> {
         return p_184389_.map((p_184395_) -> {
            return pFactory.apply(p_184395_, p_184395_);
         }, DataResult::success);
      }, (p_184411_) -> {
         P p = pMinGetter.apply(p_184411_);
         P p1 = pMaxGetter.apply(p_184411_);
         return Objects.equals(p, p1) ? Either.left(p) : Either.right(p_184411_);
      });
   }

   public static <A> Codec.ResultFunction<A> orElsePartial(final A p_184382_) {
      return new Codec.ResultFunction<A>() {
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> p_184466_, T p_184467_, DataResult<Pair<A, T>> p_184468_) {
            MutableObject<String> mutableobject = new MutableObject<>();
            Optional<Pair<A, T>> optional = p_184468_.resultOrPartial(mutableobject::setValue);
            return optional.isPresent() ? p_184468_ : DataResult.error(() -> {
               return "(" + (String)mutableobject.getValue() + " -> using default)";
            }, Pair.of(p_184382_, p_184467_));
         }

         public <T> DataResult<T> coApply(DynamicOps<T> p_184470_, A p_184471_, DataResult<T> p_184472_) {
            return p_184472_;
         }

         public String toString() {
            return "OrElsePartial[" + p_184382_ + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(ToIntFunction<E> pEncoder, IntFunction<E> pDecoder, int pNotFoundValue) {
      return Codec.INT.flatXmap((p_184414_) -> {
         return Optional.ofNullable(pDecoder.apply(p_184414_)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               return "Unknown element id: " + p_184414_;
            });
         });
      }, (p_274850_) -> {
         int i = pEncoder.applyAsInt(p_274850_);
         return i == pNotFoundValue ? DataResult.error(() -> {
            return "Element with unknown id: " + p_274850_;
         }) : DataResult.success(i);
      });
   }

   public static <E> Codec<E> stringResolverCodec(Function<E, String> pEncoder, Function<String, E> pDecoder) {
      return Codec.STRING.flatXmap((p_184404_) -> {
         return Optional.ofNullable(pDecoder.apply(p_184404_)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               return "Unknown element name:" + p_184404_;
            });
         });
      }, (p_184401_) -> {
         return Optional.ofNullable(pEncoder.apply(p_184401_)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               return "Element with unknown name: " + p_184401_;
            });
         });
      });
   }

   public static <E> Codec<E> orCompressed(final Codec<E> pFirst, final Codec<E> pSecond) {
      return new Codec<E>() {
         public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_) {
            return p_184484_.compressMaps() ? pSecond.encode(p_184483_, p_184484_, p_184485_) : pFirst.encode(p_184483_, p_184484_, p_184485_);
         }

         public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_) {
            return p_184480_.compressMaps() ? pSecond.decode(p_184480_, p_184481_) : pFirst.decode(p_184480_, p_184481_);
         }

         public String toString() {
            return pFirst + " orCompressed " + pSecond;
         }
      };
   }

   public static <E> MapCodec<E> orCompressed(final MapCodec<E> pFirst, final MapCodec<E> pSecond) {
      return new MapCodec<E>() {
         public <T> RecordBuilder<T> encode(E p_310450_, DynamicOps<T> p_312581_, RecordBuilder<T> p_310094_) {
            return p_312581_.compressMaps() ? pSecond.encode(p_310450_, p_312581_, p_310094_) : pFirst.encode(p_310450_, p_312581_, p_310094_);
         }

         public <T> DataResult<E> decode(DynamicOps<T> p_312833_, MapLike<T> p_309452_) {
            return p_312833_.compressMaps() ? pSecond.decode(p_312833_, p_309452_) : pFirst.decode(p_312833_, p_309452_);
         }

         public <T> Stream<T> keys(DynamicOps<T> p_311885_) {
            return pSecond.keys(p_311885_);
         }

         public String toString() {
            return pFirst + " orCompressed " + pSecond;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> pCodec, final Function<E, Lifecycle> p_184370_, final Function<E, Lifecycle> p_184371_) {
      return pCodec.mapResult(new Codec.ResultFunction<E>() {
         public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> p_310634_, T p_310147_, DataResult<Pair<E, T>> p_311056_) {
            return p_311056_.result().map((p_308965_) -> {
               return p_311056_.setLifecycle(p_184370_.apply(p_308965_.getFirst()));
            }).orElse(p_311056_);
         }

         public <T> DataResult<T> coApply(DynamicOps<T> p_311101_, E p_309590_, DataResult<T> p_309495_) {
            return p_309495_.setLifecycle(p_184371_.apply(p_309590_));
         }

         public String toString() {
            return "WithLifecycle[" + p_184370_ + " " + p_184371_ + "]";
         }
      });
   }

   public static <F, S> ExtraCodecs.EitherCodec<F, S> either(Codec<F> pFirst, Codec<S> pSecond) {
      return new ExtraCodecs.EitherCodec<>(pFirst, pSecond);
   }

   public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> pKey, Codec<V> pValue) {
      return new ExtraCodecs.StrictUnboundedMapCodec<>(pKey, pValue);
   }

   public static <T> Codec<T> validate(Codec<T> pCodec, Function<T, DataResult<T>> pValidator) {
      if (pCodec instanceof MapCodec.MapCodecCodec<T> mapcodeccodec) {
         return validate(mapcodeccodec.codec(), pValidator).codec();
      } else {
         return pCodec.flatXmap(pValidator, pValidator);
      }
   }

   public static <T> MapCodec<T> validate(MapCodec<T> pCodec, Function<T, DataResult<T>> pValidator) {
      return pCodec.flatXmap(pValidator, pValidator);
   }

   private static Codec<Integer> intRangeWithMessage(int pMin, int pMax, Function<Integer, String> pErrorMessage) {
      return validate(Codec.INT, (p_274889_) -> {
         return p_274889_.compareTo(pMin) >= 0 && p_274889_.compareTo(pMax) <= 0 ? DataResult.success(p_274889_) : DataResult.error(() -> {
            return pErrorMessage.apply(p_274889_);
         });
      });
   }

   public static Codec<Integer> intRange(int pMin, int pMax) {
      return intRangeWithMessage(pMin, pMax, (p_269784_) -> {
         return "Value must be within range [" + pMin + ";" + pMax + "]: " + p_269784_;
      });
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(float pMin, float pMax, Function<Float, String> pErrorMessage) {
      return validate(Codec.FLOAT, (p_274865_) -> {
         return p_274865_.compareTo(pMin) > 0 && p_274865_.compareTo(pMax) <= 0 ? DataResult.success(p_274865_) : DataResult.error(() -> {
            return pErrorMessage.apply(p_274865_);
         });
      });
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> pCodec) {
      return validate(pCodec, (p_274853_) -> {
         return p_274853_.isEmpty() ? DataResult.error(() -> {
            return "List must have contents";
         }) : DataResult.success(p_274853_);
      });
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> pCodec) {
      return validate(pCodec, (p_274860_) -> {
         return p_274860_.unwrap().right().filter(List::isEmpty).isPresent() ? DataResult.error(() -> {
            return "List must have contents";
         }) : DataResult.success(p_274860_);
      });
   }

   public static <T> Codec<T> recursive(String pName, Function<Codec<T>, Codec<T>> pWrapper) {
      return new ExtraCodecs.RecursiveCodec<>(pName, pWrapper);
   }

   public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> pDelegate) {
      return new ExtraCodecs.RecursiveCodec<>(pDelegate.toString(), (p_296623_) -> {
         return pDelegate.get();
      });
   }

   public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> pElementCodec, String pName) {
      return new ExtraCodecs.StrictOptionalFieldCodec<>(pName, pElementCodec);
   }

   public static <A> MapCodec<A> strictOptionalField(Codec<A> pElementCodec, String pName, A pFallback) {
      return strictOptionalField(pElementCodec, pName).xmap((p_296619_) -> {
         return p_296619_.orElse(pFallback);
      }, (p_296610_) -> {
         return Objects.equals(p_296610_, pFallback) ? Optional.empty() : Optional.of(p_296610_);
      });
   }

   public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> p_203977_) {
      class ContextRetrievalCodec extends MapCodec<E> {
         public <T> RecordBuilder<T> encode(E p_203993_, DynamicOps<T> p_203994_, RecordBuilder<T> p_203995_) {
            return p_203995_;
         }

         public <T> DataResult<E> decode(DynamicOps<T> p_203990_, MapLike<T> p_203991_) {
            return p_203977_.apply(p_203990_);
         }

         public String toString() {
            return "ContextRetrievalCodec[" + p_203977_ + "]";
         }

         public <T> Stream<T> keys(DynamicOps<T> p_203997_) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> pTypeGetter) {
      return (p_203980_) -> {
         Iterator<E> iterator = p_203980_.iterator();
         if (iterator.hasNext()) {
            T t = pTypeGetter.apply(iterator.next());

            while(iterator.hasNext()) {
               E e = iterator.next();
               T t1 = pTypeGetter.apply(e);
               if (t1 != t) {
                  return DataResult.error(() -> {
                     return "Mixed type list: element " + e + " had type " + t1 + ", but list is of type " + t;
                  });
               }
            }
         }

         return DataResult.success(p_203980_, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(final Codec<A> pCodec) {
      return Codec.of(pCodec, new Decoder<A>() {
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_309963_, T p_309877_) {
            try {
               return pCodec.decode(p_309963_, p_309877_);
            } catch (Exception exception) {
               return DataResult.error(() -> {
                  return "Caught exception decoding " + p_309877_ + ": " + exception.getMessage();
               });
            }
         }
      });
   }

   public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter pDateTimeFormatter) {
      return Codec.STRING.comapFlatMap((p_296605_) -> {
         try {
            return DataResult.success(pDateTimeFormatter.parse(p_296605_));
         } catch (Exception exception) {
            return DataResult.error(exception::getMessage);
         }
      }, pDateTimeFormatter::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> pCodec) {
      return pCodec.xmap(toOptionalLong, fromOptionalLong);
   }

   public static Codec<String> sizeLimitedString(int pMinSize, int pMaxSize) {
      return validate(Codec.STRING, (p_274879_) -> {
         int i = p_274879_.length();
         if (i < pMinSize) {
            return DataResult.error(() -> {
               return "String \"" + p_274879_ + "\" is too short: " + i + ", expected range [" + pMinSize + "-" + pMaxSize + "]";
            });
         } else {
            return i > pMaxSize ? DataResult.error(() -> {
               return "String \"" + p_274879_ + "\" is too long: " + i + ", expected range [" + pMinSize + "-" + pMaxSize + "]";
            }) : DataResult.success(p_274879_);
         }
      });
   }

   public static <T> Codec<T> withAlternative(Codec<T> pCodec, Codec<? extends T> pAlternative) {
      return Codec.either(pCodec, pAlternative).xmap((p_184355_) -> {
         return p_184355_.map((p_184461_) -> {
            return p_184461_;
         }, (p_184455_) -> {
            return p_184455_;
         });
      }, Either::left);
   }

   public static <T, U> Codec<T> withAlternative(Codec<T> pCodec, Codec<U> pAlternative, Function<U, T> pConverter) {
      return Codec.either(pCodec, pAlternative).xmap((p_296621_) -> {
         return p_296621_.map((p_300206_) -> {
            return p_300206_;
         }, pConverter);
      }, Either::left);
   }

   public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> pCodec) {
      return Codec.unboundedMap(pCodec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
   }

   /** @deprecated */
   @Deprecated
   public static <K, V> MapCodec<V> dispatchOptionalValue(final String p_311089_, final String p_310965_, final Codec<K> p_310912_, final Function<? super V, ? extends K> p_311760_, final Function<? super K, ? extends Codec<? extends V>> p_312960_) {
      return new MapCodec<V>() {
         public <T> Stream<T> keys(DynamicOps<T> p_310901_) {
            return Stream.of(p_310901_.createString(p_311089_), p_310901_.createString(p_310965_));
         }

         public <T> DataResult<V> decode(DynamicOps<T> p_310472_, MapLike<T> p_310342_) {
            T t = p_310342_.get(p_311089_);
            return t == null ? DataResult.error(() -> {
               return "Missing \"" + p_311089_ + "\" in: " + p_310342_;
            }) : p_310912_.decode(p_310472_, t).flatMap((p_311444_) -> {
               T t1 = Objects.requireNonNullElseGet(p_310342_.get(p_310965_), p_310472_::emptyMap);
               return p_312960_.apply(p_311444_.getFirst()).decode(p_310472_, t1).map(Pair::getFirst);
            });
         }

         public <T> RecordBuilder<T> encode(V p_309380_, DynamicOps<T> p_311460_, RecordBuilder<T> p_311592_) {
            K k = p_311760_.apply(p_309380_);
            p_311592_.add(p_311089_, p_310912_.encodeStart(p_311460_, k));
            DataResult<T> dataresult = this.encode((Codec)p_312960_.apply(k), p_309380_, p_311460_);
            if (dataresult.result().isEmpty() || !Objects.equals(dataresult.result().get(), p_311460_.emptyMap())) {
               p_311592_.add(p_310965_, dataresult);
            }

            return p_311592_;
         }

         private <T, V2 extends V> DataResult<T> encode(Codec<V2> p_313212_, V p_310224_, DynamicOps<T> p_311229_) {
            return p_313212_.encodeStart(p_311229_, (V2)p_310224_);
         }
      };
   }

   public static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
      private final Codec<F> first;
      private final Codec<S> second;

      public EitherCodec(Codec<F> pFirst, Codec<S> pSecond) {
         this.first = pFirst;
         this.second = pSecond;
      }

      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> pOps, T pInput) {
         DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(pOps, pInput).map((p_184524_) -> {
            return p_184524_.mapFirst(Either::left);
         });
         if (dataresult.error().isEmpty()) {
            return dataresult;
         } else {
            DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(pOps, pInput).map((p_184515_) -> {
               return p_184515_.mapFirst(Either::right);
            });
            return dataresult1.error().isEmpty() ? dataresult1 : dataresult.apply2((p_184517_, p_184518_) -> {
               return p_184518_;
            }, dataresult1);
         }
      }

      public <T> DataResult<T> encode(Either<F, S> pInput, DynamicOps<T> pOps, T pPrefix) {
         return pInput.map((p_184528_) -> {
            return this.first.encode(p_184528_, pOps, pPrefix);
         }, (p_184522_) -> {
            return this.second.encode(p_184522_, pOps, pPrefix);
         });
      }

      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther != null && this.getClass() == pOther.getClass()) {
            ExtraCodecs.EitherCodec<?, ?> eithercodec = (ExtraCodecs.EitherCodec)pOther;
            return Objects.equals(this.first, eithercodec.first) && Objects.equals(this.second, eithercodec.second);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(this.first, this.second);
      }

      public String toString() {
         return "EitherCodec[" + this.first + ", " + this.second + "]";
      }
   }

   static class RecursiveCodec<T> implements Codec<T> {
      private final String name;
      private final Supplier<Codec<T>> wrapped;

      RecursiveCodec(String pName, Function<Codec<T>, Codec<T>> pWrapper) {
         this.name = pName;
         this.wrapped = Suppliers.memoize(() -> {
            return pWrapper.apply(this);
         });
      }

      public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> pOps, S pInput) {
         return this.wrapped.get().decode(pOps, pInput);
      }

      public <S> DataResult<S> encode(T pInput, DynamicOps<S> pOps, S pValue) {
         return this.wrapped.get().encode(pInput, pOps, pValue);
      }

      public String toString() {
         return "RecursiveCodec[" + this.name + "]";
      }
   }

   static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
      private final String name;
      private final Codec<A> elementCodec;

      public StrictOptionalFieldCodec(String pName, Codec<A> pElementCodec) {
         this.name = pName;
         this.elementCodec = pElementCodec;
      }

      public <T> DataResult<Optional<A>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
         T t = pInput.get(this.name);
         return t == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(pOps, t).map(Optional::of);
      }

      public <T> RecordBuilder<T> encode(Optional<A> pInput, DynamicOps<T> pOps, RecordBuilder<T> pPrefix) {
         return pInput.isPresent() ? pPrefix.add(this.name, this.elementCodec.encodeStart(pOps, pInput.get())) : pPrefix;
      }

      public <T> Stream<T> keys(DynamicOps<T> pOps) {
         return Stream.of(pOps.createString(this.name));
      }

      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (!(pOther instanceof ExtraCodecs.StrictOptionalFieldCodec)) {
            return false;
         } else {
            ExtraCodecs.StrictOptionalFieldCodec<?> strictoptionalfieldcodec = (ExtraCodecs.StrictOptionalFieldCodec)pOther;
            return Objects.equals(this.name, strictoptionalfieldcodec.name) && Objects.equals(this.elementCodec, strictoptionalfieldcodec.elementCodec);
         }
      }

      public int hashCode() {
         return Objects.hash(this.name, this.elementCodec);
      }

      public String toString() {
         return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
      }
   }

   public static record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
      public <T> DataResult<Map<K, V>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
         ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

         for(Pair<T, T> pair : pInput.entries().toList()) {
            DataResult<K> dataresult = this.keyCodec().parse(pOps, pair.getFirst());
            DataResult<V> dataresult1 = this.elementCodec().parse(pOps, pair.getSecond());
            DataResult<Pair<K, V>> dataresult2 = dataresult.apply2stable(Pair::of, dataresult1);
            if (dataresult2.error().isPresent()) {
               return DataResult.error(() -> {
                  DataResult.PartialResult<Pair<K, V>> partialresult = dataresult2.error().get();
                  String s;
                  if (dataresult.result().isPresent()) {
                     s = "Map entry '" + dataresult.result().get() + "' : " + partialresult.message();
                  } else {
                     s = partialresult.message();
                  }

                  return s;
               });
            }

            if (!dataresult2.result().isPresent()) {
               return DataResult.error(() -> {
                  return "Empty or invalid map contents are not allowed";
               });
            }

            Pair<K, V> pair1 = dataresult2.result().get();
            builder.put(pair1.getFirst(), pair1.getSecond());
         }

         Map<K, V> map = builder.build();
         return DataResult.success(map);
      }

      public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> pOps, T pInput) {
         return pOps.getMap(pInput).setLifecycle(Lifecycle.stable()).flatMap((p_297301_) -> {
            return this.decode(pOps, p_297301_);
         }).map((p_300226_) -> {
            return Pair.of(p_300226_, pInput);
         });
      }

      public <T> DataResult<T> encode(Map<K, V> pInput, DynamicOps<T> pOps, T pValue) {
         return this.encode(pInput, pOps, pOps.mapBuilder()).build(pValue);
      }

      public String toString() {
         return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
      }

      public Codec<K> keyCodec() {
         return this.keyCodec;
      }

      public Codec<V> elementCodec() {
         return this.elementCodec;
      }
   }

   public static record TagOrElementLocation(ResourceLocation id, boolean tag) {
      public String toString() {
         return this.decoratedId();
      }

      private String decoratedId() {
         return this.tag ? "#" + this.id : this.id.toString();
      }
   }

   static record XorCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> pOps, T pInput) {
         DataResult<Pair<Either<F, S>, T>> dataresult = this.first.decode(pOps, pInput).map((p_144673_) -> {
            return p_144673_.mapFirst(Either::left);
         });
         DataResult<Pair<Either<F, S>, T>> dataresult1 = this.second.decode(pOps, pInput).map((p_144667_) -> {
            return p_144667_.mapFirst(Either::right);
         });
         Optional<Pair<Either<F, S>, T>> optional = dataresult.result();
         Optional<Pair<Either<F, S>, T>> optional1 = dataresult1.result();
         if (optional.isPresent() && optional1.isPresent()) {
            return DataResult.error(() -> {
               return "Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional1.get();
            }, optional.get());
         } else if (optional.isPresent()) {
            return dataresult;
         } else {
            return optional1.isPresent() ? dataresult1 : dataresult.apply2((p_296626_, p_296627_) -> {
               return p_296627_;
            }, dataresult1);
         }
      }

      public <T> DataResult<T> encode(Either<F, S> pInput, DynamicOps<T> pOps, T pPrefix) {
         return pInput.map((p_144677_) -> {
            return this.first.encode(p_144677_, pOps, pPrefix);
         }, (p_144671_) -> {
            return this.second.encode(p_144671_, pOps, pPrefix);
         });
      }

      public String toString() {
         return "XorCodec[" + this.first + ", " + this.second + "]";
      }
   }
}