package me.pepperbell.continuity.client.resource;

import java.io.FileNotFoundException;
import java.util.Properties;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class EmissiveSuffixLoader {
	public static final Identifier LOCATION = new Identifier("optifine/emissive.properties");

	private static String emissiveSuffix;

	@Nullable
	public static String getEmissiveSuffix() {
		return emissiveSuffix;
	}

	@ApiStatus.Internal
	public static void load(ResourceManager manager) {
		emissiveSuffix = null;

		try (Resource resource = manager.getResource(LOCATION)) {
			Properties properties = new Properties();
			properties.load(resource.getInputStream());
			emissiveSuffix = properties.getProperty("suffix.emissive");
		} catch (FileNotFoundException e) {
			//
		} catch (Exception e) {
			ContinuityClient.LOGGER.error("Failed to load emissive suffix from file '" + LOCATION + "'", e);
		}
	}
}
