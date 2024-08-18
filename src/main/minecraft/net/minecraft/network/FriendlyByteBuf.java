package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FriendlyByteBuf extends ByteBuf {
   public static final int DEFAULT_NBT_QUOTA = 2097152;
   private final ByteBuf source;
   public static final short MAX_STRING_LENGTH = Short.MAX_VALUE;
   public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
   private static final int PUBLIC_KEY_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_LENGTH = 512;
   private static final Gson GSON = new Gson();

   public FriendlyByteBuf(ByteBuf pSource) {
      this.source = pSource;
   }

   /** @deprecated */
   @Deprecated
   public <T> T readWithCodecTrusted(DynamicOps<Tag> pOps, Codec<T> pCodec) {
      return this.readWithCodec(pOps, pCodec, NbtAccounter.unlimitedHeap());
   }

   /** @deprecated */
   @Deprecated
   public <T> T readWithCodec(DynamicOps<Tag> pOps, Codec<T> pCodec, NbtAccounter pNbtAccounter) {
      Tag tag = this.readNbt(pNbtAccounter);
      return Util.getOrThrow(pCodec.parse(pOps, tag), (p_261423_) -> {
         return new DecoderException("Failed to decode: " + p_261423_ + " " + tag);
      });
   }

   /** @deprecated */
   @Deprecated
   public <T> FriendlyByteBuf writeWithCodec(DynamicOps<Tag> pOps, Codec<T> pCodec, T pValue) {
      Tag tag = Util.getOrThrow(pCodec.encodeStart(pOps, pValue), (p_272384_) -> {
         return new EncoderException("Failed to encode: " + p_272384_ + " " + pValue);
      });
      this.writeNbt(tag);
      return this;
   }

   public <T> T readJsonWithCodec(Codec<T> pCodec) {
      JsonElement jsonelement = GsonHelper.fromJson(GSON, this.readUtf(), JsonElement.class);
      DataResult<T> dataresult = pCodec.parse(JsonOps.INSTANCE, jsonelement);
      return Util.getOrThrow(dataresult, (p_272382_) -> {
         return new DecoderException("Failed to decode json: " + p_272382_);
      });
   }

   public <T> void writeJsonWithCodec(Codec<T> pCodec, T pValue) {
      DataResult<JsonElement> dataresult = pCodec.encodeStart(JsonOps.INSTANCE, pValue);
      this.writeUtf(GSON.toJson(Util.getOrThrow(dataresult, (p_261421_) -> {
         return new EncoderException("Failed to encode: " + p_261421_ + " " + pValue);
      })));
   }

   public <T> void writeId(IdMap<T> pIdMap, T pValue) {
      int i = pIdMap.getId(pValue);
      if (i == -1) {
         throw new IllegalArgumentException("Can't find id for '" + pValue + "' in map " + pIdMap);
      } else {
         this.writeVarInt(i);
      }
   }

   public <T> void writeId(IdMap<Holder<T>> pIdMap, Holder<T> pValue, FriendlyByteBuf.Writer<T> pWriter) {
      switch (pValue.kind()) {
         case REFERENCE:
            int i = pIdMap.getId(pValue);
            if (i == -1) {
               throw new IllegalArgumentException("Can't find id for '" + pValue.value() + "' in map " + pIdMap);
            }

            this.writeVarInt(i + 1);
            break;
         case DIRECT:
            this.writeVarInt(0);
            pWriter.accept(this, pValue.value());
      }

   }

   @Nullable
   public <T> T readById(IdMap<T> pIdMap) {
      int i = this.readVarInt();
      return pIdMap.byId(i);
   }

   public <T> Holder<T> readById(IdMap<Holder<T>> pIdMap, FriendlyByteBuf.Reader<T> pReader) {
      int i = this.readVarInt();
      if (i == 0) {
         return Holder.direct(pReader.apply(this));
      } else {
         Holder<T> holder = pIdMap.byId(i - 1);
         if (holder == null) {
            throw new IllegalArgumentException("Can't find element with id " + i);
         } else {
            return holder;
         }
      }
   }

   public static <T> IntFunction<T> limitValue(IntFunction<T> pFunction, int pLimit) {
      return (p_182686_) -> {
         if (p_182686_ > pLimit) {
            throw new DecoderException("Value " + p_182686_ + " is larger than limit " + pLimit);
         } else {
            return pFunction.apply(p_182686_);
         }
      };
   }

   public <T, C extends Collection<T>> C readCollection(IntFunction<C> pCollectionFactory, FriendlyByteBuf.Reader<T> pElementReader) {
      int i = this.readVarInt();
      C c = pCollectionFactory.apply(i);

      for(int j = 0; j < i; ++j) {
         c.add(pElementReader.apply(this));
      }

      return c;
   }

   public <T> void writeCollection(Collection<T> pCollection, FriendlyByteBuf.Writer<T> pElementWriter) {
      this.writeVarInt(pCollection.size());

      for(T t : pCollection) {
         pElementWriter.accept(this, t);
      }

   }

   public <T> List<T> readList(FriendlyByteBuf.Reader<T> pElementReader) {
      return this.readCollection(Lists::newArrayListWithCapacity, pElementReader);
   }

   public IntList readIntIdList() {
      int i = this.readVarInt();
      IntList intlist = new IntArrayList();

      for(int j = 0; j < i; ++j) {
         intlist.add(this.readVarInt());
      }

      return intlist;
   }

   public void writeIntIdList(IntList pItIdList) {
      this.writeVarInt(pItIdList.size());
      pItIdList.forEach((java.util.function.IntConsumer)this::writeVarInt);
   }

   public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> pMapFactory, FriendlyByteBuf.Reader<K> pKeyReader, FriendlyByteBuf.Reader<V> pValueReader) {
      int i = this.readVarInt();
      M m = pMapFactory.apply(i);

      for(int j = 0; j < i; ++j) {
         K k = pKeyReader.apply(this);
         V v = pValueReader.apply(this);
         m.put(k, v);
      }

      return m;
   }

   public <K, V> Map<K, V> readMap(FriendlyByteBuf.Reader<K> pKeyReader, FriendlyByteBuf.Reader<V> pValueReader) {
      return this.readMap(Maps::newHashMapWithExpectedSize, pKeyReader, pValueReader);
   }

   public <K, V> void writeMap(Map<K, V> pMap, FriendlyByteBuf.Writer<K> pKeyWriter, FriendlyByteBuf.Writer<V> pValueWriter) {
      this.writeVarInt(pMap.size());
      pMap.forEach((p_236856_, p_236857_) -> {
         pKeyWriter.accept(this, p_236856_);
         pValueWriter.accept(this, p_236857_);
      });
   }

   public void readWithCount(Consumer<FriendlyByteBuf> pReader) {
      int i = this.readVarInt();

      for(int j = 0; j < i; ++j) {
         pReader.accept(this);
      }

   }

   public <E extends Enum<E>> void writeEnumSet(EnumSet<E> pEnumSet, Class<E> pEnumClass) {
      E[] ae = pEnumClass.getEnumConstants();
      BitSet bitset = new BitSet(ae.length);

      for(int i = 0; i < ae.length; ++i) {
         bitset.set(i, pEnumSet.contains(ae[i]));
      }

      this.writeFixedBitSet(bitset, ae.length);
   }

   public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> pEnumClass) {
      E[] ae = pEnumClass.getEnumConstants();
      BitSet bitset = this.readFixedBitSet(ae.length);
      EnumSet<E> enumset = EnumSet.noneOf(pEnumClass);

      for(int i = 0; i < ae.length; ++i) {
         if (bitset.get(i)) {
            enumset.add(ae[i]);
         }
      }

      return enumset;
   }

   public <T> void writeOptional(Optional<T> pOptional, FriendlyByteBuf.Writer<T> pWriter) {
      if (pOptional.isPresent()) {
         this.writeBoolean(true);
         pWriter.accept(this, pOptional.get());
      } else {
         this.writeBoolean(false);
      }

   }

   public <T> Optional<T> readOptional(FriendlyByteBuf.Reader<T> pReader) {
      return this.readBoolean() ? Optional.of(pReader.apply(this)) : Optional.empty();
   }

   @Nullable
   public <T> T readNullable(FriendlyByteBuf.Reader<T> pReader) {
      return (T)(this.readBoolean() ? pReader.apply(this) : null);
   }

   public <T> void writeNullable(@Nullable T pValue, FriendlyByteBuf.Writer<T> pWriter) {
      if (pValue != null) {
         this.writeBoolean(true);
         pWriter.accept(this, pValue);
      } else {
         this.writeBoolean(false);
      }

   }

   public <L, R> void writeEither(Either<L, R> pValue, FriendlyByteBuf.Writer<L> pLeftWriter, FriendlyByteBuf.Writer<R> pRightWriter) {
      pValue.ifLeft((p_296387_) -> {
         this.writeBoolean(true);
         pLeftWriter.accept(this, p_296387_);
      }).ifRight((p_296383_) -> {
         this.writeBoolean(false);
         pRightWriter.accept(this, p_296383_);
      });
   }

   public <L, R> Either<L, R> readEither(FriendlyByteBuf.Reader<L> pLeftReader, FriendlyByteBuf.Reader<R> pRightReader) {
      return this.readBoolean() ? Either.left(pLeftReader.apply(this)) : Either.right(pRightReader.apply(this));
   }

   public byte[] readByteArray() {
      return this.readByteArray(this.readableBytes());
   }

   public FriendlyByteBuf writeByteArray(byte[] pArray) {
      this.writeVarInt(pArray.length);
      this.writeBytes(pArray);
      return this;
   }

   public byte[] readByteArray(int pMaxLength) {
      int i = this.readVarInt();
      if (i > pMaxLength) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + pMaxLength);
      } else {
         byte[] abyte = new byte[i];
         this.readBytes(abyte);
         return abyte;
      }
   }

   public FriendlyByteBuf writeVarIntArray(int[] pArray) {
      this.writeVarInt(pArray.length);

      for(int i : pArray) {
         this.writeVarInt(i);
      }

      return this;
   }

   public int[] readVarIntArray() {
      return this.readVarIntArray(this.readableBytes());
   }

   public int[] readVarIntArray(int pMaxLength) {
      int i = this.readVarInt();
      if (i > pMaxLength) {
         throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + pMaxLength);
      } else {
         int[] aint = new int[i];

         for(int j = 0; j < aint.length; ++j) {
            aint[j] = this.readVarInt();
         }

         return aint;
      }
   }

   public FriendlyByteBuf writeLongArray(long[] pArray) {
      this.writeVarInt(pArray.length);

      for(long i : pArray) {
         this.writeLong(i);
      }

      return this;
   }

   public long[] readLongArray() {
      return this.readLongArray((long[])null);
   }

   public long[] readLongArray(@Nullable long[] pArray) {
      return this.readLongArray(pArray, this.readableBytes() / 8);
   }

   public long[] readLongArray(@Nullable long[] pArray, int pMaxLength) {
      int i = this.readVarInt();
      if (pArray == null || pArray.length != i) {
         if (i > pMaxLength) {
            throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + pMaxLength);
         }

         pArray = new long[i];
      }

      for(int j = 0; j < pArray.length; ++j) {
         pArray[j] = this.readLong();
      }

      return pArray;
   }

   public BlockPos readBlockPos() {
      return BlockPos.of(this.readLong());
   }

   public FriendlyByteBuf writeBlockPos(BlockPos pPos) {
      this.writeLong(pPos.asLong());
      return this;
   }

   public ChunkPos readChunkPos() {
      return new ChunkPos(this.readLong());
   }

   public FriendlyByteBuf writeChunkPos(ChunkPos pChunkPos) {
      this.writeLong(pChunkPos.toLong());
      return this;
   }

   public SectionPos readSectionPos() {
      return SectionPos.of(this.readLong());
   }

   public FriendlyByteBuf writeSectionPos(SectionPos pSectionPos) {
      this.writeLong(pSectionPos.asLong());
      return this;
   }

   public GlobalPos readGlobalPos() {
      ResourceKey<Level> resourcekey = this.readResourceKey(Registries.DIMENSION);
      BlockPos blockpos = this.readBlockPos();
      return GlobalPos.of(resourcekey, blockpos);
   }

   public void writeGlobalPos(GlobalPos pPos) {
      this.writeResourceKey(pPos.dimension());
      this.writeBlockPos(pPos.pos());
   }

   public Vector3f readVector3f() {
      return new Vector3f(this.readFloat(), this.readFloat(), this.readFloat());
   }

   public void writeVector3f(Vector3f pVector3f) {
      this.writeFloat(pVector3f.x());
      this.writeFloat(pVector3f.y());
      this.writeFloat(pVector3f.z());
   }

   public Quaternionf readQuaternion() {
      return new Quaternionf(this.readFloat(), this.readFloat(), this.readFloat(), this.readFloat());
   }

   public void writeQuaternion(Quaternionf pQuaternion) {
      this.writeFloat(pQuaternion.x);
      this.writeFloat(pQuaternion.y);
      this.writeFloat(pQuaternion.z);
      this.writeFloat(pQuaternion.w);
   }

   public Vec3 readVec3() {
      return new Vec3(this.readDouble(), this.readDouble(), this.readDouble());
   }

   public void writeVec3(Vec3 pVec3) {
      this.writeDouble(pVec3.x());
      this.writeDouble(pVec3.y());
      this.writeDouble(pVec3.z());
   }

   public Component readComponent() {
      return this.readWithCodec(NbtOps.INSTANCE, ComponentSerialization.CODEC, NbtAccounter.create(2097152L));
   }

   public Component readComponentTrusted() {
      return this.readWithCodecTrusted(NbtOps.INSTANCE, ComponentSerialization.CODEC);
   }

   public FriendlyByteBuf writeComponent(Component pComponent) {
      return this.writeWithCodec(NbtOps.INSTANCE, ComponentSerialization.CODEC, pComponent);
   }

   public <T extends Enum<T>> T readEnum(Class<T> pEnumClass) {
      return (pEnumClass.getEnumConstants())[this.readVarInt()];
   }

   public FriendlyByteBuf writeEnum(Enum<?> pValue) {
      return this.writeVarInt(pValue.ordinal());
   }

   public <T> T readById(IntFunction<T> pIdLookuo) {
      int i = this.readVarInt();
      return pIdLookuo.apply(i);
   }

   public <T> FriendlyByteBuf writeById(ToIntFunction<T> pIdGetter, T pValue) {
      int i = pIdGetter.applyAsInt(pValue);
      return this.writeVarInt(i);
   }

   public int readVarInt() {
      return VarInt.read(this.source);
   }

   public long readVarLong() {
      return VarLong.read(this.source);
   }

   public FriendlyByteBuf writeUUID(UUID pUuid) {
      this.writeLong(pUuid.getMostSignificantBits());
      this.writeLong(pUuid.getLeastSignificantBits());
      return this;
   }

   public UUID readUUID() {
      return new UUID(this.readLong(), this.readLong());
   }

   public FriendlyByteBuf writeVarInt(int p_130131_) {
      VarInt.write(this.source, p_130131_);
      return this;
   }

   public FriendlyByteBuf writeVarLong(long pValue) {
      VarLong.write(this.source, pValue);
      return this;
   }

   public FriendlyByteBuf writeNbt(@Nullable Tag pTag) {
      if (pTag == null) {
         pTag = EndTag.INSTANCE;
      }

      try {
         NbtIo.writeAnyTag(pTag, new ByteBufOutputStream(this));
         return this;
      } catch (IOException ioexception) {
         throw new EncoderException(ioexception);
      }
   }

   @Nullable
   public CompoundTag readNbt() {
      Tag tag = this.readNbt(NbtAccounter.create(2097152L));
      if (tag != null && !(tag instanceof CompoundTag)) {
         throw new DecoderException("Not a compound tag: " + tag);
      } else {
         return (CompoundTag)tag;
      }
   }

   @Nullable
   public Tag readNbt(NbtAccounter pNbtAccounter) {
      try {
         Tag tag = NbtIo.readAnyTag(new ByteBufInputStream(this), pNbtAccounter);
         return tag.getId() == 0 ? null : tag;
      } catch (IOException ioexception) {
         throw new EncoderException(ioexception);
      }
   }

   public FriendlyByteBuf writeItem(ItemStack pStack) {
      if (pStack.isEmpty()) {
         this.writeBoolean(false);
      } else {
         this.writeBoolean(true);
         Item item = pStack.getItem();
         this.writeId(BuiltInRegistries.ITEM, item);
         this.writeByte(pStack.getCount());
         CompoundTag compoundtag = null;
         if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
            CompoundTag tag = pStack.getTag();

            if (pStack.viaFabricPlus$has1_10Tag()) {
               if (tag == null) tag = new CompoundTag();
               tag.putByte(ClientsideFixes.ITEM_COUNT_NBT_TAG, (byte) pStack.viaFabricPlus$get1_10Count());
            }

            compoundtag = tag;
         }

         this.writeNbt(compoundtag);
      }

      return this;
   }

   public ItemStack readItem() {
      if (!this.readBoolean()) {
         return ItemStack.EMPTY;
      } else {
         CompoundTag tag = this.readNbt();
         Item item = this.readById(BuiltInRegistries.ITEM);
         int i = this.readByte();
         ItemStack itemstack = new ItemStack(item, i);
         if (tag != null && tag.contains(ClientsideFixes.ITEM_COUNT_NBT_TAG, Tag.TAG_BYTE) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
            itemstack.viaFabricPlus$set1_10Count(tag.getByte(ClientsideFixes.ITEM_COUNT_NBT_TAG));
            tag.remove(ClientsideFixes.ITEM_COUNT_NBT_TAG);
            if (tag.isEmpty()) tag = null;
         }

         itemstack.setTag(tag);
      /*   itemstack.setTag(this.readNbt());*/
         return itemstack;
      }
   }

   public String readUtf() {
      return this.readUtf(32767);
   }

   public String readUtf(int pMaxLength) {
      return Utf8String.read(this.source, pMaxLength);
   }

   public FriendlyByteBuf writeUtf(String p_130071_) {
      return this.writeUtf(p_130071_, 32767);
   }

   public FriendlyByteBuf writeUtf(String pString, int pMaxLength) {
      Utf8String.write(this.source, pString, pMaxLength);
      return this;
   }

   public ResourceLocation readResourceLocation() {
      return new ResourceLocation(this.readUtf(32767));
   }

   public FriendlyByteBuf writeResourceLocation(ResourceLocation pResourceLocation) {
      this.writeUtf(pResourceLocation.toString());
      return this;
   }

   public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> pRegistryKey) {
      ResourceLocation resourcelocation = this.readResourceLocation();
      return ResourceKey.create(pRegistryKey, resourcelocation);
   }

   public void writeResourceKey(ResourceKey<?> pResourceKey) {
      this.writeResourceLocation(pResourceKey.location());
   }

   public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
      ResourceLocation resourcelocation = this.readResourceLocation();
      return ResourceKey.createRegistryKey(resourcelocation);
   }

   public Date readDate() {
      return new Date(this.readLong());
   }

   public FriendlyByteBuf writeDate(Date pTime) {
      this.writeLong(pTime.getTime());
      return this;
   }

   public Instant readInstant() {
      return Instant.ofEpochMilli(this.readLong());
   }

   public void writeInstant(Instant pInstant) {
      this.writeLong(pInstant.toEpochMilli());
   }

   public PublicKey readPublicKey() {
      try {
         return Crypt.byteToPublicKey(this.readByteArray(512));
      } catch (CryptException cryptexception) {
         throw new DecoderException("Malformed public key bytes", cryptexception);
      }
   }

   public FriendlyByteBuf writePublicKey(PublicKey pPublicKey) {
      this.writeByteArray(pPublicKey.getEncoded());
      return this;
   }

   public BlockHitResult readBlockHitResult() {
      BlockPos blockpos = this.readBlockPos();
      Direction direction = this.readEnum(Direction.class);
      float f = this.readFloat();
      float f1 = this.readFloat();
      float f2 = this.readFloat();
      boolean flag = this.readBoolean();
      return new BlockHitResult(new Vec3((double)blockpos.getX() + (double)f, (double)blockpos.getY() + (double)f1, (double)blockpos.getZ() + (double)f2), direction, blockpos, flag);
   }

   public void writeBlockHitResult(BlockHitResult pResult) {
      BlockPos blockpos = pResult.getBlockPos();
      this.writeBlockPos(blockpos);
      this.writeEnum(pResult.getDirection());
      Vec3 vec3 = pResult.getLocation();
      this.writeFloat((float)(vec3.x - (double)blockpos.getX()));
      this.writeFloat((float)(vec3.y - (double)blockpos.getY()));
      this.writeFloat((float)(vec3.z - (double)blockpos.getZ()));
      this.writeBoolean(pResult.isInside());
   }

   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   public void writeBitSet(BitSet pBitSet) {
      this.writeLongArray(pBitSet.toLongArray());
   }

   public BitSet readFixedBitSet(int pSize) {
      byte[] abyte = new byte[Mth.positiveCeilDiv(pSize, 8)];
      this.readBytes(abyte);
      return BitSet.valueOf(abyte);
   }

   public void writeFixedBitSet(BitSet pBitSet, int pSize) {
      if (pBitSet.length() > pSize) {
         throw new EncoderException("BitSet is larger than expected size (" + pBitSet.length() + ">" + pSize + ")");
      } else {
         byte[] abyte = pBitSet.toByteArray();
         this.writeBytes(Arrays.copyOf(abyte, Mth.positiveCeilDiv(pSize, 8)));
      }
   }

   public GameProfile readGameProfile() {
      UUID uuid = this.readUUID();
      String s = this.readUtf(16);
      GameProfile gameprofile = new GameProfile(uuid, s);
      gameprofile.getProperties().putAll(this.readGameProfileProperties());
      return gameprofile;
   }

   public void writeGameProfile(GameProfile pGameProfile) {
      this.writeUUID(pGameProfile.getId());
      this.writeUtf(pGameProfile.getName());
      this.writeGameProfileProperties(pGameProfile.getProperties());
   }

   public PropertyMap readGameProfileProperties() {
      PropertyMap propertymap = new PropertyMap();
      this.readWithCount((p_296385_) -> {
         Property property = this.readProperty();
         propertymap.put(property.name(), property);
      });
      return propertymap;
   }

   public void writeGameProfileProperties(PropertyMap pGameProfileProperties) {
      this.writeCollection(pGameProfileProperties.values(), FriendlyByteBuf::writeProperty);
   }

   public Property readProperty() {
      String s = this.readUtf();
      String s1 = this.readUtf();
      String s2 = this.readNullable(FriendlyByteBuf::readUtf);
      return new Property(s, s1, s2);
   }

   public void writeProperty(Property p_236806_) {
      this.writeUtf(p_236806_.name());
      this.writeUtf(p_236806_.value());
      this.writeNullable(p_236806_.signature(), FriendlyByteBuf::writeUtf);
   }

   public boolean isContiguous() {
      return this.source.isContiguous();
   }

   public int maxFastWritableBytes() {
      return this.source.maxFastWritableBytes();
   }

   public int capacity() {
      return this.source.capacity();
   }

   public FriendlyByteBuf capacity(int pNewCapacity) {
      this.source.capacity(pNewCapacity);
      return this;
   }

   public int maxCapacity() {
      return this.source.maxCapacity();
   }

   public ByteBufAllocator alloc() {
      return this.source.alloc();
   }

   public ByteOrder order() {
      return this.source.order();
   }

   public ByteBuf order(ByteOrder pEndianness) {
      return this.source.order(pEndianness);
   }

   public ByteBuf unwrap() {
      return this.source;
   }

   public boolean isDirect() {
      return this.source.isDirect();
   }

   public boolean isReadOnly() {
      return this.source.isReadOnly();
   }

   public ByteBuf asReadOnly() {
      return this.source.asReadOnly();
   }

   public int readerIndex() {
      return this.source.readerIndex();
   }

   public FriendlyByteBuf readerIndex(int pReaderIndex) {
      this.source.readerIndex(pReaderIndex);
      return this;
   }

   public int writerIndex() {
      return this.source.writerIndex();
   }

   public FriendlyByteBuf writerIndex(int pWriterIndex) {
      this.source.writerIndex(pWriterIndex);
      return this;
   }

   public FriendlyByteBuf setIndex(int pReaderIndex, int pWriterIndex) {
      this.source.setIndex(pReaderIndex, pWriterIndex);
      return this;
   }

   public int readableBytes() {
      return this.source.readableBytes();
   }

   public int writableBytes() {
      return this.source.writableBytes();
   }

   public int maxWritableBytes() {
      return this.source.maxWritableBytes();
   }

   public boolean isReadable() {
      return this.source.isReadable();
   }

   public boolean isReadable(int pSize) {
      return this.source.isReadable(pSize);
   }

   public boolean isWritable() {
      return this.source.isWritable();
   }

   public boolean isWritable(int pSize) {
      return this.source.isWritable(pSize);
   }

   public FriendlyByteBuf clear() {
      this.source.clear();
      return this;
   }

   public FriendlyByteBuf markReaderIndex() {
      this.source.markReaderIndex();
      return this;
   }

   public FriendlyByteBuf resetReaderIndex() {
      this.source.resetReaderIndex();
      return this;
   }

   public FriendlyByteBuf markWriterIndex() {
      this.source.markWriterIndex();
      return this;
   }

   public FriendlyByteBuf resetWriterIndex() {
      this.source.resetWriterIndex();
      return this;
   }

   public FriendlyByteBuf discardReadBytes() {
      this.source.discardReadBytes();
      return this;
   }

   public FriendlyByteBuf discardSomeReadBytes() {
      this.source.discardSomeReadBytes();
      return this;
   }

   public FriendlyByteBuf ensureWritable(int pSize) {
      this.source.ensureWritable(pSize);
      return this;
   }

   public int ensureWritable(int pSize, boolean pForce) {
      return this.source.ensureWritable(pSize, pForce);
   }

   public boolean getBoolean(int pIndex) {
      return this.source.getBoolean(pIndex);
   }

   public byte getByte(int pIndex) {
      return this.source.getByte(pIndex);
   }

   public short getUnsignedByte(int pIndex) {
      return this.source.getUnsignedByte(pIndex);
   }

   public short getShort(int pIndex) {
      return this.source.getShort(pIndex);
   }

   public short getShortLE(int pIndex) {
      return this.source.getShortLE(pIndex);
   }

   public int getUnsignedShort(int pIndex) {
      return this.source.getUnsignedShort(pIndex);
   }

   public int getUnsignedShortLE(int pIndex) {
      return this.source.getUnsignedShortLE(pIndex);
   }

   public int getMedium(int pIndex) {
      return this.source.getMedium(pIndex);
   }

   public int getMediumLE(int pIndex) {
      return this.source.getMediumLE(pIndex);
   }

   public int getUnsignedMedium(int pIndex) {
      return this.source.getUnsignedMedium(pIndex);
   }

   public int getUnsignedMediumLE(int pIndex) {
      return this.source.getUnsignedMediumLE(pIndex);
   }

   public int getInt(int pIndex) {
      return this.source.getInt(pIndex);
   }

   public int getIntLE(int pIndex) {
      return this.source.getIntLE(pIndex);
   }

   public long getUnsignedInt(int pIndex) {
      return this.source.getUnsignedInt(pIndex);
   }

   public long getUnsignedIntLE(int pIndex) {
      return this.source.getUnsignedIntLE(pIndex);
   }

   public long getLong(int pIndex) {
      return this.source.getLong(pIndex);
   }

   public long getLongLE(int pIndex) {
      return this.source.getLongLE(pIndex);
   }

   public char getChar(int pIndex) {
      return this.source.getChar(pIndex);
   }

   public float getFloat(int pIndex) {
      return this.source.getFloat(pIndex);
   }

   public double getDouble(int pIndex) {
      return this.source.getDouble(pIndex);
   }

   public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination) {
      this.source.getBytes(pIndex, pDestination);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination, int pLength) {
      this.source.getBytes(pIndex, pDestination, pLength);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination, int pDestinationIndex, int pLength) {
      this.source.getBytes(pIndex, pDestination, pDestinationIndex, pLength);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, byte[] pDestination) {
      this.source.getBytes(pIndex, pDestination);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, byte[] pDestination, int pDestinationIndex, int pLength) {
      this.source.getBytes(pIndex, pDestination, pDestinationIndex, pLength);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, ByteBuffer pDestination) {
      this.source.getBytes(pIndex, pDestination);
      return this;
   }

   public FriendlyByteBuf getBytes(int pIndex, OutputStream pOut, int pLength) throws IOException {
      this.source.getBytes(pIndex, pOut, pLength);
      return this;
   }

   public int getBytes(int pIndex, GatheringByteChannel pOut, int pLength) throws IOException {
      return this.source.getBytes(pIndex, pOut, pLength);
   }

   public int getBytes(int pIndex, FileChannel pOut, long pPosition, int pLength) throws IOException {
      return this.source.getBytes(pIndex, pOut, pPosition, pLength);
   }

   public CharSequence getCharSequence(int pIndex, int pLength, Charset pCharset) {
      return this.source.getCharSequence(pIndex, pLength, pCharset);
   }

   public FriendlyByteBuf setBoolean(int pIndex, boolean pValue) {
      this.source.setBoolean(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setByte(int pIndex, int pValue) {
      this.source.setByte(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setShort(int pIndex, int pValue) {
      this.source.setShort(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setShortLE(int pIndex, int pValue) {
      this.source.setShortLE(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setMedium(int pIndex, int pValue) {
      this.source.setMedium(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setMediumLE(int pIndex, int pValue) {
      this.source.setMediumLE(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setInt(int pIndex, int pValue) {
      this.source.setInt(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setIntLE(int pIndex, int pValue) {
      this.source.setIntLE(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setLong(int pIndex, long pValue) {
      this.source.setLong(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setLongLE(int pIndex, long pValue) {
      this.source.setLongLE(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setChar(int pIndex, int pValue) {
      this.source.setChar(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setFloat(int pIndex, float pValue) {
      this.source.setFloat(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setDouble(int pIndex, double pValue) {
      this.source.setDouble(pIndex, pValue);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource) {
      this.source.setBytes(pIndex, pSource);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource, int pLength) {
      this.source.setBytes(pIndex, pSource, pLength);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource, int pSourceIndex, int pLength) {
      this.source.setBytes(pIndex, pSource, pSourceIndex, pLength);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, byte[] pSource) {
      this.source.setBytes(pIndex, pSource);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, byte[] pSource, int pSourceIndex, int pLength) {
      this.source.setBytes(pIndex, pSource, pSourceIndex, pLength);
      return this;
   }

   public FriendlyByteBuf setBytes(int pIndex, ByteBuffer pSource) {
      this.source.setBytes(pIndex, pSource);
      return this;
   }

   public int setBytes(int pIndex, InputStream pIn, int pLength) throws IOException {
      return this.source.setBytes(pIndex, pIn, pLength);
   }

   public int setBytes(int pIndex, ScatteringByteChannel pIn, int pLength) throws IOException {
      return this.source.setBytes(pIndex, pIn, pLength);
   }

   public int setBytes(int pIndex, FileChannel pIn, long pPosition, int pLength) throws IOException {
      return this.source.setBytes(pIndex, pIn, pPosition, pLength);
   }

   public FriendlyByteBuf setZero(int pIndex, int pLength) {
      this.source.setZero(pIndex, pLength);
      return this;
   }

   public int setCharSequence(int pIndex, CharSequence pCharSequence, Charset pCharset) {
      return this.source.setCharSequence(pIndex, pCharSequence, pCharset);
   }

   public boolean readBoolean() {
      return this.source.readBoolean();
   }

   public byte readByte() {
      return this.source.readByte();
   }

   public short readUnsignedByte() {
      return this.source.readUnsignedByte();
   }

   public short readShort() {
      return this.source.readShort();
   }

   public short readShortLE() {
      return this.source.readShortLE();
   }

   public int readUnsignedShort() {
      return this.source.readUnsignedShort();
   }

   public int readUnsignedShortLE() {
      return this.source.readUnsignedShortLE();
   }

   public int readMedium() {
      return this.source.readMedium();
   }

   public int readMediumLE() {
      return this.source.readMediumLE();
   }

   public int readUnsignedMedium() {
      return this.source.readUnsignedMedium();
   }

   public int readUnsignedMediumLE() {
      return this.source.readUnsignedMediumLE();
   }

   public int readInt() {
      return this.source.readInt();
   }

   public int readIntLE() {
      return this.source.readIntLE();
   }

   public long readUnsignedInt() {
      return this.source.readUnsignedInt();
   }

   public long readUnsignedIntLE() {
      return this.source.readUnsignedIntLE();
   }

   public long readLong() {
      return this.source.readLong();
   }

   public long readLongLE() {
      return this.source.readLongLE();
   }

   public char readChar() {
      return this.source.readChar();
   }

   public float readFloat() {
      return this.source.readFloat();
   }

   public double readDouble() {
      return this.source.readDouble();
   }

   public ByteBuf readBytes(int pLength) {
      return this.source.readBytes(pLength);
   }

   public ByteBuf readSlice(int pLength) {
      return this.source.readSlice(pLength);
   }

   public ByteBuf readRetainedSlice(int pLength) {
      return this.source.readRetainedSlice(pLength);
   }

   public FriendlyByteBuf readBytes(ByteBuf pDestination) {
      this.source.readBytes(pDestination);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf pDestination, int pLength) {
      this.source.readBytes(pDestination, pLength);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf pDestination, int pDestinationIndex, int pLength) {
      this.source.readBytes(pDestination, pDestinationIndex, pLength);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] pDestination) {
      this.source.readBytes(pDestination);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] pDestination, int pDestinationIndex, int pLength) {
      this.source.readBytes(pDestination, pDestinationIndex, pLength);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuffer pDestination) {
      this.source.readBytes(pDestination);
      return this;
   }

   public FriendlyByteBuf readBytes(OutputStream pOut, int pLength) throws IOException {
      this.source.readBytes(pOut, pLength);
      return this;
   }

   public int readBytes(GatheringByteChannel pOut, int pLength) throws IOException {
      return this.source.readBytes(pOut, pLength);
   }

   public CharSequence readCharSequence(int pLength, Charset pCharset) {
      return this.source.readCharSequence(pLength, pCharset);
   }

   public int readBytes(FileChannel pOut, long pPosition, int pLength) throws IOException {
      return this.source.readBytes(pOut, pPosition, pLength);
   }

   public FriendlyByteBuf skipBytes(int pLength) {
      this.source.skipBytes(pLength);
      return this;
   }

   public FriendlyByteBuf writeBoolean(boolean pValue) {
      this.source.writeBoolean(pValue);
      return this;
   }

   public FriendlyByteBuf writeByte(int pValue) {
      this.source.writeByte(pValue);
      return this;
   }

   public FriendlyByteBuf writeShort(int pValue) {
      this.source.writeShort(pValue);
      return this;
   }

   public FriendlyByteBuf writeShortLE(int pValue) {
      this.source.writeShortLE(pValue);
      return this;
   }

   public FriendlyByteBuf writeMedium(int pValue) {
      this.source.writeMedium(pValue);
      return this;
   }

   public FriendlyByteBuf writeMediumLE(int pValue) {
      this.source.writeMediumLE(pValue);
      return this;
   }

   public FriendlyByteBuf writeInt(int pValue) {
      this.source.writeInt(pValue);
      return this;
   }

   public FriendlyByteBuf writeIntLE(int pValue) {
      this.source.writeIntLE(pValue);
      return this;
   }

   public FriendlyByteBuf writeLong(long pValue) {
      this.source.writeLong(pValue);
      return this;
   }

   public FriendlyByteBuf writeLongLE(long pValue) {
      this.source.writeLongLE(pValue);
      return this;
   }

   public FriendlyByteBuf writeChar(int pValue) {
      this.source.writeChar(pValue);
      return this;
   }

   public FriendlyByteBuf writeFloat(float pValue) {
      this.source.writeFloat(pValue);
      return this;
   }

   public FriendlyByteBuf writeDouble(double pValue) {
      this.source.writeDouble(pValue);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf pSource) {
      this.source.writeBytes(pSource);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf pSource, int pLength) {
      this.source.writeBytes(pSource, pLength);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf pSource, int pSourceIndex, int pLength) {
      this.source.writeBytes(pSource, pSourceIndex, pLength);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] pSource) {
      this.source.writeBytes(pSource);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] pSource, int pSourceIndex, int pLength) {
      this.source.writeBytes(pSource, pSourceIndex, pLength);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuffer pSource) {
      this.source.writeBytes(pSource);
      return this;
   }

   public int writeBytes(InputStream pIn, int pLength) throws IOException {
      return this.source.writeBytes(pIn, pLength);
   }

   public int writeBytes(ScatteringByteChannel pIn, int pLength) throws IOException {
      return this.source.writeBytes(pIn, pLength);
   }

   public int writeBytes(FileChannel pIn, long pPosition, int pLength) throws IOException {
      return this.source.writeBytes(pIn, pPosition, pLength);
   }

   public FriendlyByteBuf writeZero(int pLength) {
      this.source.writeZero(pLength);
      return this;
   }

   public int writeCharSequence(CharSequence pCharSequence, Charset pCharset) {
      return this.source.writeCharSequence(pCharSequence, pCharset);
   }

   public int indexOf(int pFromIndex, int pToIndex, byte pValue) {
      return this.source.indexOf(pFromIndex, pToIndex, pValue);
   }

   public int bytesBefore(byte pValue) {
      return this.source.bytesBefore(pValue);
   }

   public int bytesBefore(int pLength, byte pValue) {
      return this.source.bytesBefore(pLength, pValue);
   }

   public int bytesBefore(int pIndex, int pLength, byte pValue) {
      return this.source.bytesBefore(pIndex, pLength, pValue);
   }

   public int forEachByte(ByteProcessor pProcessor) {
      return this.source.forEachByte(pProcessor);
   }

   public int forEachByte(int pIndex, int pLength, ByteProcessor pProcessor) {
      return this.source.forEachByte(pIndex, pLength, pProcessor);
   }

   public int forEachByteDesc(ByteProcessor pProcessor) {
      return this.source.forEachByteDesc(pProcessor);
   }

   public int forEachByteDesc(int pIndex, int pLength, ByteProcessor pProcessor) {
      return this.source.forEachByteDesc(pIndex, pLength, pProcessor);
   }

   public ByteBuf copy() {
      return this.source.copy();
   }

   public ByteBuf copy(int pIndex, int pLength) {
      return this.source.copy(pIndex, pLength);
   }

   public ByteBuf slice() {
      return this.source.slice();
   }

   public ByteBuf retainedSlice() {
      return this.source.retainedSlice();
   }

   public ByteBuf slice(int pIndex, int pLength) {
      return this.source.slice(pIndex, pLength);
   }

   public ByteBuf retainedSlice(int pIndex, int pLength) {
      return this.source.retainedSlice(pIndex, pLength);
   }

   public ByteBuf duplicate() {
      return this.source.duplicate();
   }

   public ByteBuf retainedDuplicate() {
      return this.source.retainedDuplicate();
   }

   public int nioBufferCount() {
      return this.source.nioBufferCount();
   }

   public ByteBuffer nioBuffer() {
      return this.source.nioBuffer();
   }

   public ByteBuffer nioBuffer(int pIndex, int pLength) {
      return this.source.nioBuffer(pIndex, pLength);
   }

   public ByteBuffer internalNioBuffer(int pIndex, int pLength) {
      return this.source.internalNioBuffer(pIndex, pLength);
   }

   public ByteBuffer[] nioBuffers() {
      return this.source.nioBuffers();
   }

   public ByteBuffer[] nioBuffers(int pIndex, int pLength) {
      return this.source.nioBuffers(pIndex, pLength);
   }

   public boolean hasArray() {
      return this.source.hasArray();
   }

   public byte[] array() {
      return this.source.array();
   }

   public int arrayOffset() {
      return this.source.arrayOffset();
   }

   public boolean hasMemoryAddress() {
      return this.source.hasMemoryAddress();
   }

   public long memoryAddress() {
      return this.source.memoryAddress();
   }

   public String toString(Charset pCharset) {
      return this.source.toString(pCharset);
   }

   public String toString(int pIndex, int pLength, Charset pCharset) {
      return this.source.toString(pIndex, pLength, pCharset);
   }

   public int hashCode() {
      return this.source.hashCode();
   }

   public boolean equals(Object pOther) {
      return this.source.equals(pOther);
   }

   public int compareTo(ByteBuf pOther) {
      return this.source.compareTo(pOther);
   }

   public String toString() {
      return this.source.toString();
   }

   public FriendlyByteBuf retain(int pIncrement) {
      this.source.retain(pIncrement);
      return this;
   }

   public FriendlyByteBuf retain() {
      this.source.retain();
      return this;
   }

   public FriendlyByteBuf touch() {
      this.source.touch();
      return this;
   }

   public FriendlyByteBuf touch(Object pHint) {
      this.source.touch(pHint);
      return this;
   }

   public int refCnt() {
      return this.source.refCnt();
   }

   public boolean release() {
      return this.source.release();
   }

   public boolean release(int pDecrement) {
      return this.source.release(pDecrement);
   }

   @FunctionalInterface
   public interface Reader<T> extends Function<FriendlyByteBuf, T> {
      default FriendlyByteBuf.Reader<Optional<T>> asOptional() {
         return (p_236878_) -> {
            return p_236878_.readOptional(this);
         };
      }
   }

   @FunctionalInterface
   public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {
      default FriendlyByteBuf.Writer<Optional<T>> asOptional() {
         return (p_236881_, p_236882_) -> {
            p_236881_.writeOptional(p_236882_, this);
         };
      }
   }
}