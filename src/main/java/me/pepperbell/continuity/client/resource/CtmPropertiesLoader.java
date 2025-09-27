```java
package me.pepperbell.continuity.client.resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourcePackMetadata;
import net.minecraft.util.Identifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.CtmLoader;
import me.pepperbell.continuity.api.client.CtmLoaderRegistry;
import me.pepperbell.continuity.api.client.CtmProperties;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.util.BooleanState;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;

public class CtmPropertiesLoader {
	private final ResourceManager resourceManager;
	private final List<LoadingContainer<?>> containers = new ObjectArrayList<>();
	private final Map<Identifier, Set<Identifier>> textureDependencies = new Object2ObjectOpenHashMap<>();
	// MTS-specific configuration: valid CTM path sets and resource pack identifiers
	private static final String MTS_NAMESPACE = "mts";
	private static final String MTS_PACK_METADATA_KEY = "mts";
	private static final String MTS_VALID_CTM_KEY = "validCtmPaths";
	private final Map<String, Set<String>> mtsPackValidCtmPaths = new Object2ObjectOpenHashMap<>(); // Pack name -> valid CTM paths

	private CtmPropertiesLoader(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
		loadMtsValidCtmPaths(); // Load valid MTS CTM paths during initialization
	}

	public static LoadingResult loadAllWithState(ResourceManager resourceManager) {
		BiomeHolderManager.clearCache();
		LoadingResult result = loadAll(resourceManager);
		BiomeHolderManager.refreshHolders();
		return result;
	}

	public static LoadingResult loadAll(ResourceManager resourceManager) {
		return new CtmPropertiesLoader(resourceManager).loadAll();
	}

	// Load valid CTM paths for each MTS resource pack (parsed from pack.mcmeta)
	private void loadMtsValidCtmPaths() {
		for (ResourcePack pack : resourceManager.streamResourcePacks().toList()) {
			try {
				ResourcePackMetadata metadata = pack.getMetadata();
				if (metadata != null && metadata.getRaw().has(MTS_PACK_METADATA_KEY)) {
					JsonObject mtsMeta = metadata.getRaw().getAsJsonObject(MTS_PACK_METADATA_KEY);
					if (mtsMeta.has(MTS_VALID_CTM_KEY)) {
						Set<String> validCtmPaths = new ObjectOpenHashSet<>();
						mtsMeta.getAsJsonArray(MTS_VALID_CTM_KEY).forEach(jsonElem -> {
							String path = jsonElem.getAsString();
							validCtmPaths.add(path);
						});
						// Store valid paths by resource pack name (prevent cross-pack contamination)
						mtsPackValidCtmPaths.put(pack.getName(), validCtmPaths);
					}
				}
			} catch (Exception e) {
				ContinuityClient.LOGGER.warn("Failed to load MTS valid CTM paths for pack: {}", pack.getName(), e);
			}
		}
	}

	// Core validation: Check if CTM resource is a valid MTS path (newly added)
	private boolean isMtsCtmValid(ResourcePack pack, Identifier resourceId) {
		// Allow non-MTS resource packs without validation
		if (!mtsPackValidCtmPaths.containsKey(pack.getName())) {
			return true;
		}
		// MTS resource packs require path validation against allowed list
		Set<String> validPaths = mtsPackValidCtmPaths.get(pack.getName());
		return validPaths.contains(resourceId.getPath()) 
				|| validPaths.contains(resourceId.toString());
	}

	private LoadingResult loadAll() {
		int packPriority = 0;
		Iterator<ResourcePack> iterator = resourceManager.streamResourcePacks().iterator();
		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		while (iterator.hasNext()) {
			ResourcePack pack = iterator.next();
			loadAll(pack, packPriority);
			packPriority++;
		}
		invalidIdentifierState.disable();

		containers.sort(Comparator.reverseOrder());
		return new LoadingResult(containers, textureDependencies);
	}

	private void loadAll(ResourcePack pack, int packPriority) {
		for (String namespace : pack.getNamespaces(ResourceType.CLIENT_RESOURCES)) {
			// New 1: Only process CTMs in MTS or Continuity namespaces (reduce unnecessary loading)
			if (!MTS_NAMESPACE.equals(namespace) && !"continuity".equals(namespace)) {
				continue;
			}

			pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, "optifine/ctm", (resourceId, inputSupplier) -> {
				// Original format validation (retained)
				if (!resourceId.getPath().endsWith(".properties")) {
					return;
				}

				// New 2: Validate MTS resource pack path legitimacy
				if (!isMtsCtmValid(pack, resourceId)) {
					ContinuityClient.LOGGER.debug("Skipping invalid MTS CTM: {} in pack: {}", resourceId, pack.getName());
					return;
				}

				try (InputStream stream = inputSupplier.get()) {
					// New 3: Check for null input stream (prevent NPE)
					if (stream == null) {
						ContinuityClient.LOGGER.warn("Null input stream for CTM: {} in pack: {}", resourceId, pack.getName());
						return;
					}

					Properties properties = new Properties();
					properties.load(stream);
					load(properties, resourceId, pack, packPriority);
				} catch (Exception e) {
					// Enhanced error logging: explicitly label resource pack and path for easier troubleshooting
					ContinuityClient.LOGGER.error("Failed to load CTM properties from '{}' in pack '{}'", resourceId, pack.getName(), e);
				}
			});
		}
	}

	// Original load method (retained, added MTS texture dependency validation)
	private void load(Properties properties, Identifier resourceId, ResourcePack pack, int packPriority) {
		String method = properties.getProperty("method", "ctm").trim();
		CtmLoader<?> loader = CtmLoaderRegistry.get().getLoader(method);
		if (loader != null) {
			load(loader, properties, resourceId, pack, packPriority, method);
		} else {
			ContinuityClient.LOGGER.error("Unknown 'method' '{}' in CTM '{}' (pack: {})", method, resourceId, pack.getName());
		}
	}

	private <T extends CtmProperties> void load(CtmLoader<T> loader, Properties properties, Identifier resourceId, ResourcePack pack, int packPriority, String method) {
		T ctmProperties = loader.getPropertiesFactory().createProperties(properties, resourceId, pack, packPriority, resourceManager, method);
		if (ctmProperties != null) {
			// New: Validate that textures dependent on CTM are valid MTS resources
			for (var spriteId : ctmProperties.getTextureDependencies()) {
				if (!isMtsCtmValid(pack, spriteId.getTextureId())) {
					ContinuityClient.LOGGER.warn("CTM '{}' (pack: {}) depends on invalid MTS texture: {}", resourceId, pack.getName(), spriteId.getTextureId());
					return; // Skip loading this CTM if it depends on invalid textures
				}
			}

			LoadingContainer<T> container = new LoadingContainer<>(loader, ctmProperties);
			containers.add(container);
			for (var spriteId : ctmProperties.getTextureDependencies()) {
				Set<Identifier> atlasDependencies = textureDependencies.computeIfAbsent(spriteId.getAtlasId(), id -> new ObjectOpenHashSet<>());
				atlasDependencies.add(spriteId.getTextureId());
			}
		}
	}

	// Original inner class (retained, no modifications)
	private record LoadingContainer<T extends CtmProperties>(CtmLoader<T> loader, T properties) implements Comparable<LoadingContainer<?>> {
		public QuadProcessors.ProcessorHolder toProcessorHolder(Function<SpriteIdentifier, Sprite> textureGetter) {
			QuadProcessor processor = loader.getProcessorFactory().createProcessor(properties, textureGetter);
			CachingPredicates predicates = loader.getPredicatesFactory().createPredicates(properties, textureGetter);
			return new QuadProcessors.ProcessorHolder(processor, predicates);
		}

		@Override
		public int compareTo(@NotNull LoadingContainer<?> o) {
			return properties.compareTo(o.properties);
		}
	}

	// Original result class (retained, no modifications)
	public static class LoadingResult {
		private final List<LoadingContainer<?>> containers;
		private final Map<Identifier, Set<Identifier>> textureDependencies;

		private LoadingResult(List<LoadingContainer<?>> containers, Map<Identifier, Set<Identifier>> textureDependencies) {
			this.containers = containers;
			this.textureDependencies = textureDependencies;
		}

		public List<QuadProcessors.ProcessorHolder> createProcessorHolders(Function<SpriteIdentifier, Sprite> textureGetter) {
			List<QuadProcessors.ProcessorHolder> holders = new ObjectArrayList<>();
			for (var container : containers) {
				holders.add(container.toProcessorHolder(textureGetter));
			}
			return holders;
		}

		public Map<Identifier, Set<Identifier>> getTextureDependencies() {
			return textureDependencies;
		}
	}
}
```
