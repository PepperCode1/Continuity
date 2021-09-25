package me.pepperbell.continuity.api.client;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.CTMLoaderRegistryImpl;

public interface CTMLoaderRegistry {
	CTMLoaderRegistry INSTANCE = CTMLoaderRegistryImpl.INSTANCE;

	void registerLoader(String method, CTMLoader<?> loader);

	@Nullable
	CTMLoader<?> getLoader(String method);
}
