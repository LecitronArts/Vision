package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.VisibleForTesting;

public interface HolderSet<T> extends Iterable<Holder<T>> {
   Stream<Holder<T>> stream();

   int size();

   Either<TagKey<T>, List<Holder<T>>> unwrap();

   Optional<Holder<T>> getRandomElement(RandomSource pRandom);

   Holder<T> get(int pIndex);

   boolean contains(Holder<T> pHolder);

   boolean canSerializeIn(HolderOwner<T> pOwner);

   Optional<TagKey<T>> unwrapKey();

   /** @deprecated */
   @Deprecated
   @VisibleForTesting
   static <T> HolderSet.Named<T> emptyNamed(HolderOwner<T> pOwner, TagKey<T> pKey) {
      return new HolderSet.Named<>(pOwner, pKey);
   }

   @SafeVarargs
   static <T> HolderSet.Direct<T> direct(Holder<T>... pContents) {
      return new HolderSet.Direct<>(List.of(pContents));
   }

   static <T> HolderSet.Direct<T> direct(List<? extends Holder<T>> pContents) {
      return new HolderSet.Direct<>(List.copyOf(pContents));
   }

   @SafeVarargs
   static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> pHolderFactory, E... pValues) {
      return direct(Stream.of(pValues).map(pHolderFactory).toList());
   }

   static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> pHolderFactory, Collection<E> pValues) {
      return direct(pValues.stream().map(pHolderFactory).toList());
   }

   public static class Direct<T> extends HolderSet.ListBacked<T> {
      private final List<Holder<T>> contents;
      @Nullable
      private Set<Holder<T>> contentsSet;

      Direct(List<Holder<T>> pContents) {
         this.contents = pContents;
      }

      protected List<Holder<T>> contents() {
         return this.contents;
      }

      public Either<TagKey<T>, List<Holder<T>>> unwrap() {
         return Either.right(this.contents);
      }

      public Optional<TagKey<T>> unwrapKey() {
         return Optional.empty();
      }

      public boolean contains(Holder<T> pHolder) {
         if (this.contentsSet == null) {
            this.contentsSet = Set.copyOf(this.contents);
         }

         return this.contentsSet.contains(pHolder);
      }

      public String toString() {
         return "DirectSet[" + this.contents + "]";
      }
   }

   public abstract static class ListBacked<T> implements HolderSet<T> {
      protected abstract List<Holder<T>> contents();

      public int size() {
         return this.contents().size();
      }

      public Spliterator<Holder<T>> spliterator() {
         return this.contents().spliterator();
      }

      public Iterator<Holder<T>> iterator() {
         return this.contents().iterator();
      }

      public Stream<Holder<T>> stream() {
         return this.contents().stream();
      }

      public Optional<Holder<T>> getRandomElement(RandomSource p_235714_) {
         return Util.getRandomSafe(this.contents(), p_235714_);
      }

      public Holder<T> get(int p_205823_) {
         return this.contents().get(p_205823_);
      }

      public boolean canSerializeIn(HolderOwner<T> p_255876_) {
         return true;
      }
   }

   public static class Named<T> extends HolderSet.ListBacked<T> {
      private final HolderOwner<T> owner;
      private final TagKey<T> key;
      private List<Holder<T>> contents = List.of();

      Named(HolderOwner<T> pOwner, TagKey<T> pKey) {
         this.owner = pOwner;
         this.key = pKey;
      }

      void bind(List<Holder<T>> pContents) {
         this.contents = List.copyOf(pContents);
      }

      public TagKey<T> key() {
         return this.key;
      }

      protected List<Holder<T>> contents() {
         return this.contents;
      }

      public Either<TagKey<T>, List<Holder<T>>> unwrap() {
         return Either.left(this.key);
      }

      public Optional<TagKey<T>> unwrapKey() {
         return Optional.of(this.key);
      }

      public boolean contains(Holder<T> pHolder) {
         return pHolder.is(this.key);
      }

      public String toString() {
         return "NamedSet(" + this.key + ")[" + this.contents + "]";
      }

      public boolean canSerializeIn(HolderOwner<T> pOwner) {
         return this.owner.canSerializeIn(pOwner);
      }
   }
}