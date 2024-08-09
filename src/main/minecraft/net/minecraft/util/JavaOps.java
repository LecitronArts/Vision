package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class JavaOps implements DynamicOps<Object> {
   public static final JavaOps INSTANCE = new JavaOps();

   private JavaOps() {
   }

   public Object empty() {
      return null;
   }

   public Object emptyMap() {
      return Map.of();
   }

   public Object emptyList() {
      return List.of();
   }

   public <U> U convertTo(DynamicOps<U> pOutOps, Object pInput) {
      if (pInput == null) {
         return pOutOps.empty();
      } else if (pInput instanceof Map) {
         return this.convertMap(pOutOps, pInput);
      } else if (pInput instanceof ByteList) {
         ByteList bytelist = (ByteList)pInput;
         return pOutOps.createByteList(ByteBuffer.wrap(bytelist.toByteArray()));
      } else if (pInput instanceof IntList) {
         IntList intlist = (IntList)pInput;
         return pOutOps.createIntList(intlist.intStream());
      } else if (pInput instanceof LongList) {
         LongList longlist = (LongList)pInput;
         return pOutOps.createLongList(longlist.longStream());
      } else if (pInput instanceof List) {
         return this.convertList(pOutOps, pInput);
      } else if (pInput instanceof String) {
         String s = (String)pInput;
         return pOutOps.createString(s);
      } else if (pInput instanceof Boolean) {
         Boolean obool = (Boolean)pInput;
         return pOutOps.createBoolean(obool);
      } else if (pInput instanceof Byte) {
         Byte obyte = (Byte)pInput;
         return pOutOps.createByte(obyte);
      } else if (pInput instanceof Short) {
         Short oshort = (Short)pInput;
         return pOutOps.createShort(oshort);
      } else if (pInput instanceof Integer) {
         Integer integer = (Integer)pInput;
         return pOutOps.createInt(integer);
      } else if (pInput instanceof Long) {
         Long olong = (Long)pInput;
         return pOutOps.createLong(olong);
      } else if (pInput instanceof Float) {
         Float f = (Float)pInput;
         return pOutOps.createFloat(f);
      } else if (pInput instanceof Double) {
         Double d0 = (Double)pInput;
         return pOutOps.createDouble(d0);
      } else if (pInput instanceof Number) {
         Number number = (Number)pInput;
         return pOutOps.createNumeric(number);
      } else {
         throw new IllegalStateException("Don't know how to convert " + pInput);
      }
   }

   public DataResult<Number> getNumberValue(Object pInput) {
      if (pInput instanceof Number number) {
         return DataResult.success(number);
      } else {
         return DataResult.error(() -> {
            return "Not a number: " + pInput;
         });
      }
   }

   public Object createNumeric(Number pValue) {
      return pValue;
   }

   public Object createByte(byte pValue) {
      return pValue;
   }

   public Object createShort(short pValue) {
      return pValue;
   }

   public Object createInt(int pValue) {
      return pValue;
   }

   public Object createLong(long pValue) {
      return pValue;
   }

   public Object createFloat(float pValue) {
      return pValue;
   }

   public Object createDouble(double pValue) {
      return pValue;
   }

   public DataResult<Boolean> getBooleanValue(Object pInput) {
      if (pInput instanceof Boolean obool) {
         return DataResult.success(obool);
      } else {
         return DataResult.error(() -> {
            return "Not a boolean: " + pInput;
         });
      }
   }

   public Object createBoolean(boolean pValue) {
      return pValue;
   }

   public DataResult<String> getStringValue(Object pInput) {
      if (pInput instanceof String s) {
         return DataResult.success(s);
      } else {
         return DataResult.error(() -> {
            return "Not a string: " + pInput;
         });
      }
   }

   public Object createString(String pValue) {
      return pValue;
   }

   public DataResult<Object> mergeToList(Object pList, Object pValue) {
      if (pList == this.empty()) {
         return DataResult.success(List.of(pValue));
      } else if (pList instanceof List) {
         List<?> list = (List)pList;
         return list.isEmpty() ? DataResult.success(List.of(pValue)) : DataResult.success(ImmutableList.builder().addAll(list).add(pValue).build());
      } else {
         return DataResult.error(() -> {
            return "Not a list: " + pList;
         });
      }
   }

   public DataResult<Object> mergeToList(Object pList, List<Object> pValues) {
      if (pList == this.empty()) {
         return DataResult.success(pValues);
      } else if (pList instanceof List) {
         List<?> list = (List)pList;
         return list.isEmpty() ? DataResult.success(pValues) : DataResult.success(ImmutableList.builder().addAll(list).addAll(pValues).build());
      } else {
         return DataResult.error(() -> {
            return "Not a list: " + pList;
         });
      }
   }

   public DataResult<Object> mergeToMap(Object pMap, Object pKey, Object pValue) {
      if (pMap == this.empty()) {
         return DataResult.success(Map.of(pKey, pValue));
      } else if (pMap instanceof Map) {
         Map<?, ?> map = (Map)pMap;
         if (map.isEmpty()) {
            return DataResult.success(Map.of(pKey, pValue));
         } else {
            ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size() + 1);
            builder.putAll(map);
            builder.put(pKey, pValue);
            return DataResult.success(builder.buildKeepingLast());
         }
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pMap;
         });
      }
   }

   public DataResult<Object> mergeToMap(Object pMap, Map<Object, Object> pValues) {
      if (pMap == this.empty()) {
         return DataResult.success(pValues);
      } else if (pMap instanceof Map) {
         Map<?, ?> map = (Map)pMap;
         if (map.isEmpty()) {
            return DataResult.success(pValues);
         } else {
            ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size() + pValues.size());
            builder.putAll(map);
            builder.putAll(pValues);
            return DataResult.success(builder.buildKeepingLast());
         }
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pMap;
         });
      }
   }

   private static Map<Object, Object> mapLikeToMap(MapLike<Object> pMapLike) {
      return pMapLike.entries().collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
   }

   public DataResult<Object> mergeToMap(Object pMap, MapLike<Object> pValues) {
      if (pMap == this.empty()) {
         return DataResult.success(mapLikeToMap(pValues));
      } else if (pMap instanceof Map) {
         Map<?, ?> map = (Map)pMap;
         if (map.isEmpty()) {
            return DataResult.success(mapLikeToMap(pValues));
         } else {
            ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size());
            builder.putAll(map);
            pValues.entries().forEach((p_311291_) -> {
               builder.put(p_311291_.getFirst(), p_311291_.getSecond());
            });
            return DataResult.success(builder.buildKeepingLast());
         }
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pMap;
         });
      }
   }

   static Stream<Pair<Object, Object>> getMapEntries(Map<?, ?> pInput) {
      return pInput.entrySet().stream().map((p_309487_) -> {
         return Pair.of(p_309487_.getKey(), p_309487_.getValue());
      });
   }

   public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object pInput) {
      if (pInput instanceof Map<?, ?> map) {
         return DataResult.success(getMapEntries(map));
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pInput;
         });
      }
   }

   public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object pInput) {
      if (pInput instanceof Map<?, ?> map) {
         return DataResult.success(map::forEach);
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pInput;
         });
      }
   }

   public Object createMap(Stream<Pair<Object, Object>> pMap) {
      return pMap.collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
   }

   public DataResult<MapLike<Object>> getMap(Object pInput) {
      if (pInput instanceof final Map<?, ?> map) {
         return DataResult.success(new MapLike<Object>() {
            @Nullable
            public Object get(Object p_310138_) {
               return map.get(p_310138_);
            }

            @Nullable
            public Object get(String p_309918_) {
               return map.get(p_309918_);
            }

            public Stream<Pair<Object, Object>> entries() {
               return JavaOps.getMapEntries(map);
            }

            public String toString() {
               return "MapLike[" + map + "]";
            }
         });
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pInput;
         });
      }
   }

   public Object createMap(Map<Object, Object> pInput) {
      return pInput;
   }

   public DataResult<Stream<Object>> getStream(Object pInput) {
      if (pInput instanceof List<?> list) {
         return DataResult.success(list.stream().map((p_312057_) -> {
            return p_312057_;
         }));
      } else {
         return DataResult.error(() -> {
            return "Not an list: " + pInput;
         });
      }
   }

   public DataResult<Consumer<Consumer<Object>>> getList(Object pInput) {
      if (pInput instanceof List<?> list) {
         return DataResult.success(list::forEach);
      } else {
         return DataResult.error(() -> {
            return "Not an list: " + pInput;
         });
      }
   }

   public Object createList(Stream<Object> pInput) {
      return pInput.toList();
   }

   public DataResult<ByteBuffer> getByteBuffer(Object pInput) {
      if (pInput instanceof ByteList bytelist) {
         return DataResult.success(ByteBuffer.wrap(bytelist.toByteArray()));
      } else {
         return DataResult.error(() -> {
            return "Not a byte list: " + pInput;
         });
      }
   }

   public Object createByteList(ByteBuffer pValue) {
      ByteBuffer bytebuffer = pValue.duplicate().clear();
      ByteArrayList bytearraylist = new ByteArrayList();
      bytearraylist.size(bytebuffer.capacity());
      bytebuffer.get(0, bytearraylist.elements(), 0, bytearraylist.size());
      return bytearraylist;
   }

   public DataResult<IntStream> getIntStream(Object pInput) {
      if (pInput instanceof IntList intlist) {
         return DataResult.success(intlist.intStream());
      } else {
         return DataResult.error(() -> {
            return "Not an int list: " + pInput;
         });
      }
   }

   public Object createIntList(IntStream pValue) {
      return IntArrayList.toList(pValue);
   }

   public DataResult<LongStream> getLongStream(Object pInput) {
      if (pInput instanceof LongList longlist) {
         return DataResult.success(longlist.longStream());
      } else {
         return DataResult.error(() -> {
            return "Not a long list: " + pInput;
         });
      }
   }

   public Object createLongList(LongStream pValue) {
      return LongArrayList.toList(pValue);
   }

   public Object remove(Object pInput, String pKey) {
      if (pInput instanceof Map<?, ?> map) {
         Map<Object, Object> map1 = new LinkedHashMap<>(map);
         map1.remove(pKey);
         return DataResult.success(Map.copyOf(map1));
      } else {
         return DataResult.error(() -> {
            return "Not a map: " + pInput;
         });
      }
   }

   public RecordBuilder<Object> mapBuilder() {
      return new JavaOps.FixedMapBuilder<>(this);
   }

   public String toString() {
      return "Java";
   }

   static final class FixedMapBuilder<T> extends RecordBuilder.AbstractUniversalBuilder<T, ImmutableMap.Builder<T, T>> {
      public FixedMapBuilder(DynamicOps<T> pOps) {
         super(pOps);
      }

      protected ImmutableMap.Builder<T, T> initBuilder() {
         return ImmutableMap.builder();
      }

      protected ImmutableMap.Builder<T, T> append(T pKey, T pValue, ImmutableMap.Builder<T, T> pBuilder) {
         return pBuilder.put(pKey, pValue);
      }

      protected DataResult<T> build(ImmutableMap.Builder<T, T> pBuilder, T pPrefix) {
         ImmutableMap<T, T> immutablemap;
         try {
            immutablemap = pBuilder.buildOrThrow();
         } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> {
               return "Can't build map: " + illegalargumentexception.getMessage();
            });
         }

         return this.ops().mergeToMap(pPrefix, immutablemap);
      }
   }
}