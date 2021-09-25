package me.pepperbell.continuity.api.client;

import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public interface CTMPropertiesFactory<T extends CTMProperties> {
	@Nullable
	T createProperties(Properties properties, Identifier id, String packName, int packPriority, String method);
}
