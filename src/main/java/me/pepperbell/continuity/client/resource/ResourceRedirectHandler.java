
package me.pepperbell.continuity.client.resource;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.metadata.ResourcePackMetadata;
import net.minecraft.util.Identifier;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.pepperbell.continuity.client.mixin.ReloadableResourceManagerImplAccessor;
import me.pepperbell.continuity.client.mixinterface.LifecycledResourceManagerImplExtension;
import me.pepperbell.continuity.client.util.BooleanState;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.HashSet;

public class ResourceRedirectHandler {
	public static final String SPRITE_PATH_START = "continuity_reserved/";
	public static final String PATH_START = "textures/" + SPRITE_PATH_START;
	public static final String PATH_END = ".png";
	public static final int PATH_START_LENGTH = PATH_START.length();
	public static final int PATH_END_LENGTH = PATH_END.length();
	public static final int HEX_LENGTH = 8;
	public static final int HEX_END = PATH_START_LENGTH + HEX_LENGTH;
	public static final int MIN_LENGTH = PATH_START_LENGTH + HEX_LENGTH + PATH_END_LENGTH;
	// MTS-specific configuration: namespace and valid resource fields in pack.mcmeta
	private static final String MTS_NAMESPACE = "mts";
	private static final String MTS_PACK_METADATA_KEY = "mts";
	private static final String MTS_VALID_RESOURCES_KEY = "validResources";

	private static final char[] HEX_BUFFER = new char[HEX_LENGTH];
	private static final char[] HEX_DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	private final ObjectList<RedirectInfo> redirects = new ObjectArrayList<>();
	private final Object2IntMap<String> indexMap = new Object2IntOpenHashMap<>();
	private final Set<String> mtsValidPaths; // Set of valid resource paths for MTS resource packs
	private int nextIndex = 0;

	{
		indexMap.defaultReturnValue(-1);
		mtsValidPaths = new HashSet<>();
	}

	// Load valid paths for MTS resource packs during initialization (parsed from ResourceManager)
	private ResourceRedirectHandler(ResourceManager resourceManager) {
		loadMtsValidPaths(resourceManager);
	}

	@Nullable
	public static ResourceRedirectHandler get(ResourceManager resourceManager) {
		if (resourceManager instanceof ReloadableResourceManagerImplAccessor accessor) {
			resourceManager = accessor.getActiveManager();
		}
		if (resourceManager instanceof LifecycledResourceManagerImplExtension extension) {
			// If not initialized, create an instance with ResourceManager to load MTS valid paths
			if (extension.continuity$getRedirectHandler() == null) {
				extension.continuity$setRedirectHandler(new ResourceRedirectHandler(resourceManager));
			}
			return extension.continuity$getRedirectHandler();
		}
		return null;
	}

	// Load valid resource paths from MTS resource packs (parsed from pack.mcmeta)
	private void loadMtsValidPaths(ResourceManager resourceManager) {
		for (ResourcePack pack : resourceManager.streamResourcePacks().toList()) {
			try {
				// 1. Check if the resource pack is MTS (determined by custom field in pack.mcmeta)
				ResourcePackMetadata metadata = pack.getMetadata();
				if (metadata != null && metadata.getRaw().has(MTS_PACK_METADATA_KEY)) {
					JsonObject mtsMeta = metadata.getRaw().getAsJsonObject(MTS_PACK_METADATA_KEY);
					// 2. Parse the "validResources" field (stores resource paths allowed by MTS)
					if (mtsMeta.has(MTS_VALID_RESOURCES_KEY)) {
						mtsMeta.getAsJsonArray(MTS_VALID_RESOURCES_KEY).forEach(jsonElem -> {
							String validPath = jsonElem.getAsString();
							mtsValidPaths.add(validPath);
						});
					}
				}
			} catch (IOException e) {
				// Ignore parsing errors for individual packs to avoid affecting overall loading
				me.pepperbell.continuity.client.ContinuityClient.LOGGER.warn("Failed to parse MTS metadata for pack: {}", pack.getName(), e);
			}
		}
	}

	// Core validation: Check if the path is a valid MTS resource (newly added)
	private boolean isValidMtsResource(String absolutePath) {
		// Case 1: Resources not in MTS namespace are allowed (only validate MTS-related resources)
		Identifier id = new Identifier(absolutePath);
		if (!MTS_NAMESPACE.equals(id.getNamespace())) {
			return true;
		}
		// Case 2: Resources in MTS namespace must be in the valid list
		return mtsValidPaths.contains(absolutePath) || mtsValidPaths.contains(id.getPath());
	}

	@Override
	public String getSourceSpritePath(String absolutePath) {
		// New: Validate MTS legitimacy first; do not assign redirect index to invalid resources
		if (!isValidMtsResource(absolutePath)) {
			me.pepperbell.continuity.client.ContinuityClient.LOGGER.debug("Skipping invalid MTS resource: {}", absolutePath);
			return absolutePath; // Return original path without redirecting
		}

		int index = indexMap.getInt(absolutePath);
		if (index == -1) {
			RedirectInfo info = RedirectInfo.of(absolutePath);
			index = nextIndex++;
			redirects.add(info);
			indexMap.put(absolutePath, index);
		}
		return SPRITE_PATH_START + toHex(index);
	}

	@Override
	public Identifier redirect(Identifier id) {
		// New 1: Only process resources in MTS or Continuity namespaces (avoid mishandling unrelated resources)
		if (!MTS_NAMESPACE.equals(id.getNamespace()) && !"continuity".equals(id.getNamespace())) {
			return id;
		}

		String path = id.getPath();
		// New 2: Validate MTS legitimacy
		if (!isValidMtsResource(id.toString())) {
			me.pepperbell.continuity.client.ContinuityClient.LOGGER.debug("Skipping redirect for invalid MTS resource: {}", id);
			return id;
		}

		// Original path format validation (retained)
		if (!path.startsWith(PATH_START) || !path.endsWith(PATH_END)) {
			return id;
		}
		int length = path.length();
		if (length < MIN_LENGTH) {
			return id;
		}

		int index = parseHex(path, PATH_START_LENGTH);
		if (index < 0 || index >= redirects.size()) {
			return id;
		}

		RedirectInfo info = redirects.get(index);
		String newPath;
		if (length == MIN_LENGTH) {
			newPath = info.defaultPath;
		} else {
			String suffix = path.substring(HEX_END, length - PATH_END_LENGTH);
			newPath = info.createPath(suffix);
		}

		// New 3: Validate the legitimacy of the redirected path (avoid generating invalid paths)
		if (!isValidMtsResource(new Identifier(id.getNamespace(), newPath).toString())) {
			me.pepperbell.continuity.client.ContinuityClient.LOGGER.warn("Redirected path is invalid for MTS: {}", newPath);
			return id;
		}

		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		Identifier newId = id.withPath(newPath);
		invalidIdentifierState.disable();

		return newId;
	}

	// Original helper methods (retained, no modifications)
	public static int parseHex(String string, int startIndex) {
		int i = 0;
		int charPos = startIndex;
		int endIndex = startIndex + HEX_LENGTH;
		while (charPos < endIndex) {
			i <<= 4;
			char c = string.charAt(charPos++);
			if (c >= '0' && c <= '9') {
				i |= c - '0';
			} else if (c >= 'a' && c <= 'f') {
				i |= c - 'a' + 10;
			} else {
				return -1;
			}
		}
		return i;
	}

	public static String toHex(int i) {
		int charPos = HEX_LENGTH;
		do {
			HEX_BUFFER[--charPos] = HEX_DIGITS[i & 15];
			i >>>= 4;
		} while (charPos > 0);
		return new String(HEX_BUFFER);
	}

	// Original inner class (retained, no modifications)
	private static abstract class RedirectInfo {
		public final String defaultPath;

		protected RedirectInfo(String defaultPath) {
			this.defaultPath = defaultPath;
		}

		public abstract String createPath(String suffix);

		public static RedirectInfo of(String path) {
			int extensionIndex = FilenameUtils.indexOfExtension(path);
			if (extensionIndex != -1) {
				String pathStart = path.substring(0, extensionIndex);
				String pathEnd = path.substring(extensionIndex);
				return new RedirectInfo(path) {
					@Override
					public String createPath(String suffix) {
						return pathStart + suffix + pathEnd;
					}
				};
			} else {
				return new RedirectInfo(path) {
					@Override
					public String createPath(String suffix) {
						return path + suffix;
					}
				};
			}
		}
	}
}
```
