package me.pepperbell.continuity.client.resource;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class ResourcePackUtil {
	private static ResourcePack[] resourcePacks;

	public static DefaultResourcePack getDefaultResourcePack() {
		return MinecraftClient.getInstance().getResourcePackProvider().getPack();
	}

	@ApiStatus.Internal
	public static void setup(ResourceManager resourceManager) {
		resourcePacks = resourceManager.streamResourcePacks().toArray(ResourcePack[]::new);
		ArrayUtils.reverse(resourcePacks);
	}

	@Nullable
	public static ResourcePack getProvidingResourcePack(Identifier resourceId) {
		for (ResourcePack resourcePack : resourcePacks) {
			if (resourcePack.contains(ResourceType.CLIENT_RESOURCES, resourceId)) {
				return resourcePack;
			}
		}
		return null;
	}

	@ApiStatus.Internal
	public static void clear() {
		resourcePacks = null;
	}
}
