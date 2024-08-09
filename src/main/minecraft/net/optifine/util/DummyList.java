package net.optifine.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DummyList<E> implements List<E> {
   public static final DummyList INSTANCE = new DummyList();

   public int size() {
      return 0;
   }

   public boolean isEmpty() {
      return true;
   }

   public boolean contains(Object o) {
      return false;
   }

   public Iterator<E> iterator() {
      return null;
   }

   public Object[] toArray() {
      return null;
   }

   public <T> T[] toArray(T[] a) {
      return null;
   }

   public boolean add(E e) {
      return false;
   }

   public boolean remove(Object o) {
      return false;
   }

   public boolean containsAll(Collection<?> c) {
      return false;
   }

   public boolean addAll(Collection<? extends E> c) {
      return false;
   }

   public boolean addAll(int index, Collection<? extends E> c) {
      return false;
   }

   public boolean removeAll(Collection<?> c) {
      return false;
   }

   public boolean retainAll(Collection<?> c) {
      return false;
   }

   public void clear() {
   }

   public E get(int index) {
      return (E)null;
   }

   public E set(int index, E element) {
      return (E)null;
   }

   public void add(int index, E element) {
   }

   public E remove(int index) {
      return (E)null;
   }

   public int indexOf(Object o) {
      return -1;
   }

   public int lastIndexOf(Object o) {
      return -1;
   }

   public ListIterator<E> listIterator() {
      return null;
   }

   public ListIterator<E> listIterator(int index) {
      return null;
   }

   public List<E> subList(int fromIndex, int toIndex) {
      return this;
   }

   public class DummyIterator<E> implements Iterator<E> {
      public boolean hasNext() {
         return false;
      }

      public E next() {
         return (E)null;
      }
   }

   public class DummyListIterator<E> implements ListIterator<E> {
      public boolean hasNext() {
         return false;
      }

      public E next() {
         return (E)null;
      }

      public boolean hasPrevious() {
         return false;
      }

      public E previous() {
         return (E)null;
      }

      public int nextIndex() {
         return 0;
      }

      public int previousIndex() {
         return 0;
      }

      public void remove() {
      }

      public void set(E e) {
      }

      public void add(E e) {
      }
   }
}