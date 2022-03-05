package me.pepperbell.continuity.client.util;

import me.pepperbell.continuity.client.mixin.ReloadableResourceManagerImplAccessor;
import me.pepperbell.continuity.client.mixinterface.LifecycledResourceManagerImplExtension;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceRedirectHelper {
	public static void addRedirect(ResourceManager resourceManager, Identifier from, Identifier to) {
		if (resourceManager instanceof ReloadableResourceManagerImplAccessor accessor) {
			resourceManager = accessor.getActiveManager();
		}
		if (resourceManager instanceof LifecycledResourceManagerImplExtension extension) {
			extension.addRedirect(from, to);
		}
	}
}
