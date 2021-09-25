package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.minecraft.util.Identifier;

public interface ProcessingDataKeyRegistry {
	ProcessingDataKeyRegistry INSTANCE = ProcessingDataKeyRegistryImpl.INSTANCE;

	default <T> ProcessingDataKey<T> registerKey(Identifier id, Supplier<T> valueSupplier) {
		return registerKey(id, valueSupplier, null);
	}

	<T> ProcessingDataKey<T> registerKey(Identifier id, Supplier<T> valueSupplier, Consumer<T> valueResetAction);

	@Nullable ProcessingDataKey<?> getKey(Identifier id);

	int getRegisteredAmount();
}
