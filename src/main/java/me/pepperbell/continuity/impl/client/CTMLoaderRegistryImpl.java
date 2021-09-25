package me.pepperbell.continuity.impl.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;

public class CTMLoaderRegistryImpl implements CTMLoaderRegistry {
	public static CTMLoaderRegistryImpl INSTANCE = new CTMLoaderRegistryImpl();

	private final Map<String, CTMLoader<?>> loaders = new Object2ObjectOpenHashMap<>();

	@Override
	public void registerLoader(String method, CTMLoader<?> loader) {
		loaders.put(method, loader);
	}

	@Override
	public @Nullable CTMLoader<?> getLoader(String method) {
		return loaders.get(method);
	}
}
