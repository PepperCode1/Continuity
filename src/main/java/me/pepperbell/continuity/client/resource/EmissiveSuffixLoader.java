package me.pepperbell.continuity.client.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class EmissiveSuffixLoader {
	public static final Identifier LOCATION = new Identifier("optifine/emissive.properties");

	private static String emissiveSuffix;

	@Nullable
	public static String getEmissiveSuffix() {
		return emissiveSuffix;
	}

	@ApiStatus.Internal
	public static void load(ResourceManager manager) {
		emissiveSuffix = null;

		Optional<Resource> optionalResource = manager.getResource(LOCATION);
		if (optionalResource.isPresent()) {
			Resource resource = optionalResource.get();
			try (InputStream inputStream = resource.getInputStream()) {
				Properties properties = new Properties();
				properties.load(inputStream);
				emissiveSuffix = properties.getProperty("suffix.emissive");
			} catch (IOException e) {
				ContinuityClient.LOGGER.error("Failed to load emissive suffix from file '" + LOCATION + "'", e);
			}
		}
	}
}
