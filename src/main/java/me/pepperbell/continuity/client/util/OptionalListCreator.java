package me.pepperbell.continuity.client.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class OptionalListCreator<T> implements Consumer<T>, Supplier<ObjectArrayList<T>> {
	private ObjectArrayList<T> list = null;

	@Override
	public void accept(T t) {
		if (list == null) {
			list = new ObjectArrayList<>();
		}
		list.add(t);
	}

	@Override
	@Nullable
	public ObjectArrayList<T> get() {
		ObjectArrayList<T> list = this.list;
		this.list = null;
		return list;
	}
}
