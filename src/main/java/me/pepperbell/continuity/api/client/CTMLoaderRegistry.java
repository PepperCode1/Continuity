package me.pepperbell.continuity.api.client;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.CTMLoaderRegistryImpl;

@ApiStatus.NonExtendable
public interface CTMLoaderRegistry {
	static CTMLoaderRegistry get() {
		return CTMLoaderRegistryImpl.INSTANCE;
	}

	void registerLoader(String method, CTMLoader<?> loader);

	@Nullable
	CTMLoader<?> getLoader(String method);
}
