package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class MappedRegistry<T> implements WritableRegistry<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final ResourceKey<? extends Registry<T>> key;
   private final ObjectList<Holder.Reference<T>> byId = new ObjectArrayList<>(256);
   private final Reference2IntMap<T> toId = Util.make(new Reference2IntOpenHashMap<>(), (p_308420_) -> {
      p_308420_.defaultReturnValue(-1);
   });
   private final Map<ResourceLocation, Holder.Reference<T>> byLocation = new HashMap<>();
   private final Map<ResourceKey<T>, Holder.Reference<T>> byKey = new HashMap<>();
   private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>();
   private final Map<T, Lifecycle> lifecycles = new IdentityHashMap<>();
   private Lifecycle registryLifecycle;
   private volatile Map<TagKey<T>, HolderSet.Named<T>> tags = new IdentityHashMap<>();
   private boolean frozen;
   @Nullable
   private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
   @Nullable
   private List<Holder.Reference<T>> holdersInOrder;
   private int nextId;
   private final HolderLookup.RegistryLookup<T> lookup = new HolderLookup.RegistryLookup<T>() {
      public ResourceKey<? extends Registry<? extends T>> key() {
         return MappedRegistry.this.key;
      }

      public Lifecycle registryLifecycle() {
         return MappedRegistry.this.registryLifecycle();
      }

      public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255624_) {
         return MappedRegistry.this.getHolder(p_255624_);
      }

      public Stream<Holder.Reference<T>> listElements() {
         return MappedRegistry.this.holders();
      }

      public Optional<HolderSet.Named<T>> get(TagKey<T> p_256277_) {
         return MappedRegistry.this.getTag(p_256277_);
      }

      public Stream<HolderSet.Named<T>> listTags() {
         return MappedRegistry.this.getTags().map(Pair::getSecond);
      }
   };

   public MappedRegistry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle) {
      this(pKey, pRegistryLifecycle, false);
   }

   public MappedRegistry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle, boolean pHasIntrusiveHolders) {
      this.key = pKey;
      this.registryLifecycle = pRegistryLifecycle;
      if (pHasIntrusiveHolders) {
         this.unregisteredIntrusiveHolders = new IdentityHashMap<>();
      }

   }

   public ResourceKey<? extends Registry<T>> key() {
      return this.key;
   }

   public String toString() {
      return "Registry[" + this.key + " (" + this.registryLifecycle + ")]";
   }

   private List<Holder.Reference<T>> holdersInOrder() {
      if (this.holdersInOrder == null) {
         this.holdersInOrder = this.byId.stream().filter(Objects::nonNull).toList();
      }

      return this.holdersInOrder;
   }

   private void validateWrite() {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen");
      }
   }

   private void validateWrite(ResourceKey<T> pKey) {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen (trying to add key " + pKey + ")");
      }
   }

   public Holder.Reference<T> registerMapping(int pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle) {
      this.validateWrite(pKey);
      Validate.notNull(pKey);
      Validate.notNull(pValue);
      if (this.byLocation.containsKey(pKey.location())) {
         Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + pKey + "' to registry"));
      }

      if (this.byValue.containsKey(pValue)) {
         Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + pValue + "' to registry"));
      }

      Holder.Reference<T> reference;
      if (this.unregisteredIntrusiveHolders != null) {
         reference = this.unregisteredIntrusiveHolders.remove(pValue);
         if (reference == null) {
            throw new AssertionError("Missing intrusive holder for " + pKey + ":" + pValue);
         }

         reference.bindKey(pKey);
      } else {
         reference = this.byKey.computeIfAbsent(pKey, (p_258168_) -> {
            return Holder.Reference.createStandAlone(this.holderOwner(), p_258168_);
         });
      }

      this.byKey.put(pKey, reference);
      this.byLocation.put(pKey.location(), reference);
      this.byValue.put(pValue, reference);
      this.byId.size(Math.max(this.byId.size(), pId + 1));
      this.byId.set(pId, reference);
      this.toId.put(pValue, pId);
      if (this.nextId <= pId) {
         this.nextId = pId + 1;
      }

      this.lifecycles.put(pValue, pLifecycle);
      this.registryLifecycle = this.registryLifecycle.add(pLifecycle);
      this.holdersInOrder = null;
      return reference;
   }

   public Holder.Reference<T> register(ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle) {
      return this.registerMapping(this.nextId, pKey, pValue, pLifecycle);
   }

   @Nullable
   public ResourceLocation getKey(T pValue) {
      Holder.Reference<T> reference = this.byValue.get(pValue);
      return reference != null ? reference.key().location() : null;
   }

   public Optional<ResourceKey<T>> getResourceKey(T pValue) {
      return Optional.ofNullable(this.byValue.get(pValue)).map(Holder.Reference::key);
   }

   public int getId(@Nullable T pValue) {
      return this.toId.getInt(pValue);
   }

   @Nullable
   public T get(@Nullable ResourceKey<T> pKey) {
      return getValueFromNullable(this.byKey.get(pKey));
   }

   @Nullable
   public T byId(int pId) {
      return (T)(pId >= 0 && pId < this.byId.size() ? getValueFromNullable(this.byId.get(pId)) : null);
   }

   public Optional<Holder.Reference<T>> getHolder(int pId) {
      return pId >= 0 && pId < this.byId.size() ? Optional.ofNullable(this.byId.get(pId)) : Optional.empty();
   }

   public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> pKey) {
      return Optional.ofNullable(this.byKey.get(pKey));
   }

   public Holder<T> wrapAsHolder(T pValue) {
      Holder.Reference<T> reference = this.byValue.get(pValue);
      return (Holder<T>)(reference != null ? reference : Holder.direct(pValue));
   }

   Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> pKey) {
      return this.byKey.computeIfAbsent(pKey, (p_258169_) -> {
         if (this.unregisteredIntrusiveHolders != null) {
            throw new IllegalStateException("This registry can't create new holders without value");
         } else {
            this.validateWrite(p_258169_);
            return Holder.Reference.createStandAlone(this.holderOwner(), p_258169_);
         }
      });
   }

   public int size() {
      return this.byKey.size();
   }

   public Lifecycle lifecycle(T pValue) {
      return this.lifecycles.get(pValue);
   }

   public Lifecycle registryLifecycle() {
      return this.registryLifecycle;
   }

   public Iterator<T> iterator() {
      return Iterators.transform(this.holdersInOrder().iterator(), Holder::value);
   }

   @Nullable
   public T get(@Nullable ResourceLocation pName) {
      Holder.Reference<T> reference = this.byLocation.get(pName);
      return getValueFromNullable(reference);
   }

   @Nullable
   private static <T> T getValueFromNullable(@Nullable Holder.Reference<T> pHolder) {
      return (T)(pHolder != null ? pHolder.value() : null);
   }

   public Set<ResourceLocation> keySet() {
      return Collections.unmodifiableSet(this.byLocation.keySet());
   }

   public Set<ResourceKey<T>> registryKeySet() {
      return Collections.unmodifiableSet(this.byKey.keySet());
   }

   public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
      return Collections.unmodifiableSet(Maps.transformValues(this.byKey, Holder::value).entrySet());
   }

   public Stream<Holder.Reference<T>> holders() {
      return this.holdersInOrder().stream();
   }

   public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
      return this.tags.entrySet().stream().map((p_211060_) -> {
         return Pair.of(p_211060_.getKey(), p_211060_.getValue());
      });
   }

   public HolderSet.Named<T> getOrCreateTag(TagKey<T> pKey) {
      HolderSet.Named<T> named = this.tags.get(pKey);
      if (named == null) {
         named = this.createTag(pKey);
         Map<TagKey<T>, HolderSet.Named<T>> map = new IdentityHashMap<>(this.tags);
         map.put(pKey, named);
         this.tags = map;
      }

      return named;
   }

   private HolderSet.Named<T> createTag(TagKey<T> p_211068_) {
      return new HolderSet.Named<>(this.holderOwner(), p_211068_);
   }

   public Stream<TagKey<T>> getTagNames() {
      return this.tags.keySet().stream();
   }

   public boolean isEmpty() {
      return this.byKey.isEmpty();
   }

   public Optional<Holder.Reference<T>> getRandom(RandomSource pRandom) {
      return Util.getRandomSafe(this.holdersInOrder(), pRandom);
   }

   public boolean containsKey(ResourceLocation pName) {
      return this.byLocation.containsKey(pName);
   }

   public boolean containsKey(ResourceKey<T> pKey) {
      return this.byKey.containsKey(pKey);
   }

   public Registry<T> freeze() {
      if (this.frozen) {
         return this;
      } else {
         this.frozen = true;
         this.byValue.forEach((p_247989_, p_247990_) -> {
            p_247990_.bindValue(p_247989_);
         });
         List<ResourceLocation> list = this.byKey.entrySet().stream().filter((p_211055_) -> {
            return !p_211055_.getValue().isBound();
         }).map((p_211794_) -> {
            return p_211794_.getKey().location();
         }).sorted().toList();
         if (!list.isEmpty()) {
            throw new IllegalStateException("Unbound values in registry " + this.key() + ": " + list);
         } else {
            if (this.unregisteredIntrusiveHolders != null) {
               if (!this.unregisteredIntrusiveHolders.isEmpty()) {
                  throw new IllegalStateException("Some intrusive holders were not registered: " + this.unregisteredIntrusiveHolders.values());
               }

               this.unregisteredIntrusiveHolders = null;
            }

            return this;
         }
      }
   }

   public Holder.Reference<T> createIntrusiveHolder(T pValue) {
      if (this.unregisteredIntrusiveHolders == null) {
         throw new IllegalStateException("This registry can't create intrusive holders");
      } else {
         this.validateWrite();
         return this.unregisteredIntrusiveHolders.computeIfAbsent(pValue, (p_258166_) -> {
            return Holder.Reference.createIntrusive(this.asLookup(), p_258166_);
         });
      }
   }

   public Optional<HolderSet.Named<T>> getTag(TagKey<T> pKey) {
      return Optional.ofNullable(this.tags.get(pKey));
   }

   public void bindTags(Map<TagKey<T>, List<Holder<T>>> pTagMap) {
      Map<Holder.Reference<T>, List<TagKey<T>>> map = new IdentityHashMap<>();
      this.byKey.values().forEach((p_211801_) -> {
         map.put(p_211801_, new ArrayList<>());
      });
      pTagMap.forEach((p_211806_, p_211807_) -> {
         for(Holder<T> holder : p_211807_) {
            if (!holder.canSerializeIn(this.asLookup())) {
               throw new IllegalStateException("Can't create named set " + p_211806_ + " containing value " + holder + " from outside registry " + this);
            }

            if (!(holder instanceof Holder.Reference)) {
               throw new IllegalStateException("Found direct holder " + holder + " value in tag " + p_211806_);
            }

            Holder.Reference<T> reference = (Holder.Reference)holder;
            map.get(reference).add(p_211806_);
         }

      });
      Set<TagKey<T>> set = Sets.difference(this.tags.keySet(), pTagMap.keySet());
      if (!set.isEmpty()) {
         LOGGER.warn("Not all defined tags for registry {} are present in data pack: {}", this.key(), set.stream().map((p_211811_) -> {
            return p_211811_.location().toString();
         }).sorted().collect(Collectors.joining(", ")));
      }

      Map<TagKey<T>, HolderSet.Named<T>> map1 = new IdentityHashMap<>(this.tags);
      pTagMap.forEach((p_211797_, p_211798_) -> {
         map1.computeIfAbsent(p_211797_, this::createTag).bind(p_211798_);
      });
      map.forEach(Holder.Reference::bindTags);
      this.tags = map1;
   }

   public void resetTags() {
      this.tags.values().forEach((p_211792_) -> {
         p_211792_.bind(List.of());
      });
      this.byKey.values().forEach((p_211803_) -> {
         p_211803_.bindTags(Set.of());
      });
   }

   public HolderGetter<T> createRegistrationLookup() {
      this.validateWrite();
      return new HolderGetter<T>() {
         public Optional<Holder.Reference<T>> get(ResourceKey<T> p_259097_) {
            return Optional.of(this.getOrThrow(p_259097_));
         }

         public Holder.Reference<T> getOrThrow(ResourceKey<T> p_259750_) {
            return MappedRegistry.this.getOrCreateHolderOrThrow(p_259750_);
         }

         public Optional<HolderSet.Named<T>> get(TagKey<T> p_259486_) {
            return Optional.of(this.getOrThrow(p_259486_));
         }

         public HolderSet.Named<T> getOrThrow(TagKey<T> p_260298_) {
            return MappedRegistry.this.getOrCreateTag(p_260298_);
         }
      };
   }

   public HolderOwner<T> holderOwner() {
      return this.lookup;
   }

   public HolderLookup.RegistryLookup<T> asLookup() {
      return this.lookup;
   }
}