package net.minecraft.world.level.chunk;

import baritone.utils.accessor.IPalettedContainer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;
import org.spongepowered.asm.mixin.Unique;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T>, IPalettedContainer<T> {
   private static final int MIN_PALETTE_BITS = 0;
   private final PaletteResize<T> dummyPaletteResize = (p_198182_0_, p_198182_1_) -> {
      return 0;
   };
   private final IdMap<T> registry;
   private volatile PalettedContainer.Data<T> data;
   private final PalettedContainer.Strategy strategy;
   private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

   // Mixin has no way of referring to the data field and we can't use inheritance
   // tricks to determine its name, so we use this ugly workaround instead.
   // Classloading is hell here and causes accessor mixins (@Mixin interfaces with
   // only @Accessor and @Invoker methods) to break on use and proguard hates method
   // handles and on top of that mojang decided that error messages during world
   // load are not needed so if you want to debug this you'll probably need an extra
   // mixin just to display the error and hard quit the game before follow up errors
   // blow up your log file.
   // Mumphrey, please add the shadow classes you promised 5 years ago.

   @Override
   public Palette<T> getPalette() {
      return data.palette;
   }

   @Override
   public BitStorage getStorage() {
      return data.storage;
   }


   private static <T extends Throwable> T sneaky(Throwable t, Class<T> as) throws T {
      throw (T) t;
   }


   public void acquire() {
      this.threadingDetector.checkAndLock();
   }

   public void release() {
      this.threadingDetector.checkAndUnlock();
   }

   public static <T> Codec<PalettedContainer<T>> codecRW(IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainer<T>> unpacker = PalettedContainer::unpack;
      return codec(pRegistry, pCodec, pStrategy, pValue, unpacker);
   }

   public static <T> Codec<PalettedContainerRO<T>> codecRO(IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> unpacker = (p_196881_0_, p_196881_1_, p_196881_2_) -> {
         return unpack(p_196881_0_, p_196881_1_, p_196881_2_).map((p_200428_0_) -> {
            return p_200428_0_;
         });
      };
      return codec(pRegistry, pCodec, pStrategy, pValue, unpacker);
   }

   private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue, PalettedContainerRO.Unpacker<T, C> pUnpacker) {
      return RecordCodecBuilder.<PalettedContainerRO.PackedData<T>>create((p_188044_2_) -> {
         return p_188044_2_.group(pCodec.mapResult(ExtraCodecs.orElsePartial(pValue)).listOf().fieldOf("palette").forGetter(PalettedContainerRO.PackedData::paletteEntries), Codec.LONG_STREAM.optionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)).apply(p_188044_2_, PalettedContainerRO.PackedData::new);
      }).comapFlatMap((p_188078_3_) -> {
         return pUnpacker.read(pRegistry, pStrategy, p_188078_3_);
      }, (p_188071_2_) -> {
         return p_188071_2_.pack(pRegistry, pStrategy);
      });
   }

   public PalettedContainer(IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy, PalettedContainer.Configuration<T> pConfiguration, BitStorage pStorage, List<T> pValues) {
      this.registry = pRegistry;
      this.strategy = pStrategy;
      this.data = new PalettedContainer.Data<>(pConfiguration, pStorage, pConfiguration.factory().create(pConfiguration.bits(), pRegistry, this, pValues));
   }

   private PalettedContainer(IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy, PalettedContainer.Data<T> pData) {
      this.registry = pRegistry;
      this.strategy = pStrategy;
      this.data = pData;
   }

   public PalettedContainer(IdMap<T> pRegistry, T pPalette, PalettedContainer.Strategy pStrategy) {
      this.strategy = pStrategy;
      this.registry = pRegistry;
      this.data = this.createOrReuseData((PalettedContainer.Data)null, 0);
      this.data.palette.idFor(pPalette);
   }

   private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> pData, int pId) {
      PalettedContainer.Configuration<T> configuration = this.strategy.getConfiguration(this.registry, pId);
      return pData != null && configuration.equals(pData.configuration()) ? pData : configuration.createData(this.registry, this, this.strategy.size());
   }

   public int onResize(int pBits, T pObjectAdded) {
      PalettedContainer.Data<T> data = this.data;
      PalettedContainer.Data<T> data1 = this.createOrReuseData(data, pBits);
      data1.copyFrom(data.palette, data.storage);
      this.data = data1;
      return data1.palette.idFor(pObjectAdded);
   }

   public T getAndSet(int pX, int pY, int pZ, T pState) {
      this.acquire();

      Object object;
      try {
         object = this.getAndSet(this.strategy.getIndex(pX, pY, pZ), pState);
      } finally {
         this.release();
      }

      return (T)object;
   }

   public T getAndSetUnchecked(int pX, int pY, int pZ, T pState) {
      return this.getAndSet(this.strategy.getIndex(pX, pY, pZ), pState);
   }

   private T getAndSet(int pIndex, T pState) {
      int i = this.data.palette.idFor(pState);
      int j = this.data.storage.getAndSet(pIndex, i);
      return this.data.palette.valueFor(j);
   }

   public void set(int pX, int pY, int pZ, T pState) {
      this.acquire();

      try {
         this.set(this.strategy.getIndex(pX, pY, pZ), pState);
      } finally {
         this.release();
      }

   }

   private void set(int pIndex, T pState) {
      int i = this.data.palette.idFor(pState);
      this.data.storage.set(pIndex, i);
   }

   public T get(int pX, int pY, int pZ) {
      return this.get(this.strategy.getIndex(pX, pY, pZ));
   }

   protected T get(int pIndex) {
      PalettedContainer.Data<T> data = this.data;
      return data.palette.valueFor(data.storage.get(pIndex));
   }

   public void getAll(Consumer<T> pConsumer) {
      Palette<T> palette = this.data.palette();
      IntSet intset = new IntArraySet();
      this.data.storage.getAll(intset::add);
      intset.forEach((p_196885_2_) -> {
         pConsumer.accept(palette.valueFor(p_196885_2_));
      });
   }

   public void read(FriendlyByteBuf pBuffer) {
      this.acquire();

      try {
         int i = pBuffer.readByte();
         PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
         data.palette.read(pBuffer);
         pBuffer.readLongArray(data.storage.getRaw());
         this.data = data;
      } finally {
         this.release();
      }

   }

   public void write(FriendlyByteBuf pBuffer) {
      this.acquire();

      try {
         this.data.write(pBuffer);
      } finally {
         this.release();
      }

   }

   private static <T> DataResult<PalettedContainer<T>> unpack(IdMap<T> p_188068_, PalettedContainer.Strategy p_188069_, PalettedContainerRO.PackedData<T> p_238258_) {
      List<T> list = p_238258_.paletteEntries();
      int i = p_188069_.size();
      int j = p_188069_.calculateBitsForSerialization(p_188068_, list.size());
      PalettedContainer.Configuration<T> configuration = p_188069_.getConfiguration(p_188068_, j);
      BitStorage bitstorage;
      if (j == 0) {
         bitstorage = new ZeroBitStorage(i);
      } else {
         Optional<LongStream> optional = p_238258_.storage();
         if (optional.isEmpty()) {
            return DataResult.error(() -> {
               return "Missing values for non-zero storage";
            });
         }

         long[] along = optional.get().toArray();

         try {
            if (configuration.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY) {
               Palette<T> palette = new HashMapPalette<>(p_188068_, j, (p_238277_0_, p_238277_1_) -> {
                  return 0;
               }, list);
               SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, along);
               int[] aint = new int[i];
               simplebitstorage.unpack(aint);
               swapPalette(aint, (p_238280_2_) -> {
                  return p_188068_.getId(palette.valueFor(p_238280_2_));
               });
               bitstorage = new SimpleBitStorage(configuration.bits(), i, aint);
            } else {
               bitstorage = new SimpleBitStorage(configuration.bits(), i, along);
            }
         } catch (SimpleBitStorage.InitializationException simplebitstorage$initializationexception1) {
            return DataResult.error(() -> {
               return "Failed to read PalettedContainer: " + simplebitstorage$initializationexception1.getMessage();
            });
         }
      }

      return DataResult.success(new PalettedContainer<>(p_188068_, p_188069_, configuration, bitstorage, list));
   }

   public PalettedContainerRO.PackedData<T> pack(IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy) {
      this.acquire();

      PalettedContainerRO.PackedData palettedcontainerro$packeddata;
      try {
         HashMapPalette<T> hashmappalette = new HashMapPalette<>(pRegistry, this.data.storage.getBits(), this.dummyPaletteResize);
         int i = pStrategy.size();
         int[] aint = new int[i];
         this.data.storage.unpack(aint);
         swapPalette(aint, (p_198176_2_) -> {
            return hashmappalette.idFor(this.data.palette.valueFor(p_198176_2_));
         });
         int j = pStrategy.calculateBitsForSerialization(pRegistry, hashmappalette.getSize());
         Optional<LongStream> optional;
         if (j != 0) {
            SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, aint);
            optional = Optional.of(Arrays.stream(simplebitstorage.getRaw()));
         } else {
            optional = Optional.empty();
         }

         palettedcontainerro$packeddata = new PalettedContainerRO.PackedData<>(hashmappalette.getEntries(), optional);
      } finally {
         this.release();
      }

      return palettedcontainerro$packeddata;
   }

   private static <T> void swapPalette(int[] pBits, IntUnaryOperator pOperator) {
      int i = -1;
      int j = -1;

      for(int k = 0; k < pBits.length; ++k) {
         int l = pBits[k];
         if (l != i) {
            i = l;
            j = pOperator.applyAsInt(l);
         }

         pBits[k] = j;
      }

   }

   public int getSerializedSize() {
      return this.data.getSerializedSize();
   }

   public boolean maybeHas(Predicate<T> pPredicate) {
      return this.data.palette.maybeHas(pPredicate);
   }

   public PalettedContainer<T> copy() {
      return new PalettedContainer<>(this.registry, this.strategy, this.data.copy());
   }

   public PalettedContainer<T> recreate() {
      return new PalettedContainer<>(this.registry, this.data.palette.valueFor(0), this.strategy);
   }

   public void count(PalettedContainer.CountConsumer<T> pCountConsumer) {
      if (this.data.palette.getSize() == 1) {
         pCountConsumer.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
      } else {
         Int2IntOpenHashMap int2intopenhashmap = new Int2IntOpenHashMap();
         this.data.storage.getAll((p_198179_1_) -> {
            int2intopenhashmap.addTo(p_198179_1_, 1);
         });
         int2intopenhashmap.int2IntEntrySet().forEach((p_63138_2_) -> {
            pCountConsumer.accept(this.data.palette.valueFor(p_63138_2_.getIntKey()), p_63138_2_.getIntValue());
         });
      }

   }

   public void finish() {
      this.data.storage().finish();
   }

   static record Configuration<T>(Palette.Factory factory, int bits) {
      public PalettedContainer.Data<T> createData(IdMap<T> pRegistry, PaletteResize<T> pPaletteResize, int pSize) {
         BitStorage bitstorage = (BitStorage)(this.bits == 0 ? new ZeroBitStorage(pSize) : new SimpleBitStorage(this.bits, pSize));
         Palette<T> palette = this.factory.create(this.bits, pRegistry, pPaletteResize, List.of());
         return new PalettedContainer.Data<>(this, bitstorage, palette);
      }

      public Palette.Factory factory() {
         return this.factory;
      }

      public int bits() {
         return this.bits;
      }
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(T pState, int pCount);
   }

   static record Data<T>(PalettedContainer.Configuration<T> configuration, BitStorage storage, Palette<T> palette) {
      public void copyFrom(Palette<T> pPalette, BitStorage pBitStorage) {
         for(int i = 0; i < pBitStorage.getSize(); ++i) {
            T t = pPalette.valueFor(pBitStorage.get(i));
            this.storage.set(i, this.palette.idFor(t));
         }

      }

      public int getSerializedSize() {
         return 1 + this.palette.getSerializedSize() + VarInt.getByteSize(this.storage.getRaw().length) + this.storage.getRaw().length * 8;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeByte(this.storage.getBits());
         this.palette.write(pBuffer);
         pBuffer.writeLongArray(this.storage.getRaw());
      }

      public PalettedContainer.Data<T> copy() {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
      }

      public PalettedContainer.Configuration<T> configuration() {
         return this.configuration;
      }

      public BitStorage storage() {
         return this.storage;
      }

      public Palette<T> palette() {
         return this.palette;
      }
   }

   public abstract static class Strategy {
      public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
      public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
      public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
      static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
      public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4) {
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188157_, int p_188158_) {
            PalettedContainer.Configuration palettedcontainer$configuration;
            switch (p_188158_) {
               case 0:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188158_);
                  break;
               case 1:
               case 2:
               case 3:
               case 4:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);
                  break;
               case 5:
               case 6:
               case 7:
               case 8:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, p_188158_);
                  break;
               default:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188157_.size()));
            }

            return palettedcontainer$configuration;
         }
      };
      public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2) {
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188162_, int p_188163_) {
            PalettedContainer.Configuration palettedcontainer$configuration;
            switch (p_188163_) {
               case 0:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188163_);
                  break;
               case 1:
               case 2:
               case 3:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, p_188163_);
                  break;
               default:
                  palettedcontainer$configuration = new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188162_.size()));
            }

            return palettedcontainer$configuration;
         }
      };
      private final int sizeBits;

      Strategy(int pSizeBits) {
         this.sizeBits = pSizeBits;
      }

      public int size() {
         return 1 << this.sizeBits * 3;
      }

      public int getIndex(int pX, int pY, int pZ) {
         return (pY << this.sizeBits | pZ) << this.sizeBits | pX;
      }

      public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> pRegistry, int pSize);

      <A> int calculateBitsForSerialization(IdMap<A> pRegistry, int pSize) {
         int i = Mth.ceillog2(pSize);
         PalettedContainer.Configuration<A> configuration = this.getConfiguration(pRegistry, i);
         return configuration.factory() == GLOBAL_PALETTE_FACTORY ? i : configuration.bits();
      }
   }
}
