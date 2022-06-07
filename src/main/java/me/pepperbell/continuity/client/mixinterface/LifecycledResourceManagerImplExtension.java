package me.pepperbell.continuity.client.mixinterface;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;

public interface LifecycledResourceManagerImplExtension {
	@Nullable
	ResourceRedirectHandler getRedirectHandler();
}
