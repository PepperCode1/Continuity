package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public interface ProcessingDataKey<T> {
	Identifier getId();

	int getRawId();

	Supplier<T> getValueSupplier();

	@Nullable
	Consumer<T> getValueResetAction();
}
