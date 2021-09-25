package me.pepperbell.continuity.client.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class VoidSet<T> implements Set<T> {
	public static final VoidSet<?> INSTANCE = new VoidSet<>();

	@SuppressWarnings("unchecked")
	public static <T> VoidSet<T> get() {
		return (VoidSet<T>) INSTANCE;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return VoidIterator.get();
	}

	@Override
	public Object[] toArray() {
		return new Object[0];
	}

	@Override
	public <V> V[] toArray(V[] a) {
		return a;
	}

	@Override
	public boolean add(T e) {
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return c.isEmpty();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {
	}

	public static class VoidIterator<T> implements Iterator<T> {
		public static final VoidIterator<?> INSTANCE = new VoidIterator<>();

		@SuppressWarnings("unchecked")
		public static <T> VoidIterator<T> get() {
			return (VoidIterator<T>) INSTANCE;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			throw new NoSuchElementException();
		}
	}
}
