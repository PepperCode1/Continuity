package me.pepperbell.continuity.impl.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataKey;
import net.minecraft.util.Identifier;

public record ProcessingDataKeyImpl<T>(Identifier id, int rawId, Supplier<T> valueSupplier, Consumer<T> valueResetAction) implements ProcessingDataKey<T> {
	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public int getRawId() {
		return rawId;
	}

	@Override
	public Supplier<T> getValueSupplier() {
		return valueSupplier;
	}

	@Override
	@Nullable
	public Consumer<T> getValueResetAction() {
		return valueResetAction;
	}
}
