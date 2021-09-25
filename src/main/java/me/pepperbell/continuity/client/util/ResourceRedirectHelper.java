package me.pepperbell.continuity.client.util;

import me.pepperbell.continuity.client.mixinterface.ReloadableResourceManagerImplExtension;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceRedirectHelper {
	public static void addRedirect(ResourceManager resourceManager, Identifier from, Identifier to) {
		if (resourceManager instanceof ReloadableResourceManagerImplExtension extension) {
			extension.addRedirect(from, to);
		}
	}
}
