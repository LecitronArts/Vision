package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

public class ArrayListDeque<T> extends AbstractList<T> implements Serializable, RandomAccess {
   private static final int MIN_GROWTH = 1;
   private Object[] contents;
   private int head;
   private int size;

   public ArrayListDeque() {
      this(1);
   }

   public ArrayListDeque(int pSize) {
      this.contents = new Object[pSize];
      this.head = 0;
      this.size = 0;
   }

   public int size() {
      return this.size;
   }

   @VisibleForTesting
   public int capacity() {
      return this.contents.length;
   }

   private int getIndex(int pIndex) {
      return (pIndex + this.head) % this.contents.length;
   }

   public T get(int pIndex) {
      this.verifyIndexInRange(pIndex);
      return this.getInner(this.getIndex(pIndex));
   }

   private static void verifyIndexInRange(int pIndex, int pSize) {
      if (pIndex < 0 || pIndex >= pSize) {
         throw new IndexOutOfBoundsException(pIndex);
      }
   }

   private void verifyIndexInRange(int pIndex) {
      verifyIndexInRange(pIndex, this.size);
   }

   private T getInner(int pIndex) {
      return (T)this.contents[pIndex];
   }

   public T set(int pIndex, T pValue) {
      this.verifyIndexInRange(pIndex);
      Objects.requireNonNull(pValue);
      int i = this.getIndex(pIndex);
      T t = this.getInner(i);
      this.contents[i] = pValue;
      return t;
   }

   public void add(int pIndex, T pElement) {
      verifyIndexInRange(pIndex, this.size + 1);
      Objects.requireNonNull(pElement);
      if (this.size == this.contents.length) {
         this.grow();
      }

      int i = this.getIndex(pIndex);
      if (pIndex == this.size) {
         this.contents[i] = pElement;
      } else if (pIndex == 0) {
         --this.head;
         if (this.head < 0) {
            this.head += this.contents.length;
         }

         this.contents[this.getIndex(0)] = pElement;
      } else {
         for(int j = this.size - 1; j >= pIndex; --j) {
            this.contents[this.getIndex(j + 1)] = this.contents[this.getIndex(j)];
         }

         this.contents[i] = pElement;
      }

      ++this.modCount;
      ++this.size;
   }

   private void grow() {
      int i = this.contents.length + Math.max(this.contents.length >> 1, 1);
      Object[] aobject = new Object[i];
      this.copyCount(aobject, this.size);
      this.head = 0;
      this.contents = aobject;
   }

   public T remove(int pIndex) {
      this.verifyIndexInRange(pIndex);
      int i = this.getIndex(pIndex);
      T t = this.getInner(i);
      if (pIndex == 0) {
         this.contents[i] = null;
         ++this.head;
      } else if (pIndex == this.size - 1) {
         this.contents[i] = null;
      } else {
         for(int j = pIndex + 1; j < this.size; ++j) {
            this.contents[this.getIndex(j - 1)] = this.get(j);
         }

         this.contents[this.getIndex(this.size - 1)] = null;
      }

      ++this.modCount;
      --this.size;
      return t;
   }

   public boolean removeIf(Predicate<? super T> pPredicate) {
      int i = 0;

      for(int j = 0; j < this.size; ++j) {
         T t = this.get(j);
         if (pPredicate.test(t)) {
            ++i;
         } else if (i != 0) {
            this.contents[this.getIndex(j - i)] = t;
            this.contents[this.getIndex(j)] = null;
         }
      }

      this.modCount += i;
      this.size -= i;
      return i != 0;
   }

   private void copyCount(Object[] pOutput, int pCount) {
      for(int i = 0; i < pCount; ++i) {
         pOutput[i] = this.get(i);
      }

   }

   public void replaceAll(UnaryOperator<T> pOperator) {
      for(int i = 0; i < this.size; ++i) {
         int j = this.getIndex(i);
         this.contents[j] = Objects.requireNonNull(pOperator.apply(this.getInner(i)));
      }

   }

   public void forEach(Consumer<? super T> pAction) {
      for(int i = 0; i < this.size; ++i) {
         pAction.accept(this.get(i));
      }

   }

   public void addFirst(T pElement) {
      this.add(0, pElement);
   }

   public void addLast(T pElement) {
      this.add(this.size, pElement);
   }

   public boolean offerFirst(T pElement) {
      this.addFirst(pElement);
      return true;
   }

   public boolean offerLast(T pElement) {
      this.addLast(pElement);
      return true;
   }

   public T removeFirst() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.remove(0);
      }
   }

   public T removeLast() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.remove(this.size - 1);
      }
   }

   @Nullable
   public T pollFirst() {
      return (T)(this.size == 0 ? null : this.removeFirst());
   }

   @Nullable
   public T pollLast() {
      return (T)(this.size == 0 ? null : this.removeLast());
   }

   public T getFirst() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.get(0);
      }
   }

   public T getLast() {
      if (this.size == 0) {
         throw new NoSuchElementException();
      } else {
         return this.get(this.size - 1);
      }
   }

   @Nullable
   public T peekFirst() {
      return (T)(this.size == 0 ? null : this.getFirst());
   }

   @Nullable
   public T peekLast() {
      return (T)(this.size == 0 ? null : this.getLast());
   }

   public boolean removeFirstOccurrence(Object pElement) {
      for(int i = 0; i < this.size; ++i) {
         T t = this.get(i);
         if (Objects.equals(pElement, t)) {
            this.remove(i);
            return true;
         }
      }

      return false;
   }

   public boolean removeLastOccurrence(Object pElement) {
      for(int i = this.size - 1; i >= 0; --i) {
         T t = this.get(i);
         if (Objects.equals(pElement, t)) {
            this.remove(i);
            return true;
         }
      }

      return false;
   }

   public boolean offer(T pElement) {
      return this.offerLast(pElement);
   }

   public T remove() {
      return this.removeFirst();
   }

   @Nullable
   public T poll() {
      return this.pollFirst();
   }

   public T element() {
      return this.getFirst();
   }

   @Nullable
   public T peek() {
      return this.peekFirst();
   }

   public void push(T pElement) {
      this.addFirst(pElement);
   }

   public T pop() {
      return this.removeFirst();
   }

   public Iterator<T> descendingIterator() {
      return new ArrayListDeque.DescendingIterator();
   }

   class DescendingIterator implements Iterator<T> {
      private int index = ArrayListDeque.this.size() - 1;

      public DescendingIterator() {
      }

      public boolean hasNext() {
         return this.index >= 0;
      }

      public T next() {
         return ArrayListDeque.this.get(this.index--);
      }

      public void remove() {
         ArrayListDeque.this.remove(this.index + 1);
      }
   }
}