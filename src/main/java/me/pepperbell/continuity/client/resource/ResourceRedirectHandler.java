package me.pepperbell.continuity.client.resource;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.pepperbell.continuity.client.mixin.ReloadableResourceManagerImplAccessor;
import me.pepperbell.continuity.client.mixinterface.LifecycledResourceManagerImplExtension;
import me.pepperbell.continuity.client.util.BooleanState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class ResourceRedirectHandler {
	public static final String SPRITE_PATH_START = "continuity_reserved/";
	public static final String PATH_START = "textures/" + SPRITE_PATH_START;
	public static final String PATH_END = ".png";
	public static final int PATH_START_LENGTH = PATH_START.length();
	public static final int PATH_END_LENGTH = PATH_END.length();
	public static final int HEX_LENGTH = 8;
	public static final int HEX_END = PATH_START_LENGTH + HEX_LENGTH;
	public static final int MIN_LENGTH = PATH_START_LENGTH + HEX_LENGTH + PATH_END_LENGTH;

	private static final char[] HEX_BUFFER = new char[HEX_LENGTH];
	private static final char[] HEX_DIGITS = {
			'0', '1', '2', '3',
			'4', '5', '6', '7',
			'8', '9', 'a', 'b',
			'c', 'd', 'e', 'f'
	};

	private final ObjectList<RedirectInfo> redirects = new ObjectArrayList<>();
	private final Object2IntMap<String> indexMap = new Object2IntOpenHashMap<>();
	private int nextIndex = 0;

	{
		indexMap.defaultReturnValue(-1);
	}

	@Nullable
	public static ResourceRedirectHandler get(ResourceManager resourceManager) {
		if (resourceManager instanceof ReloadableResourceManagerImplAccessor accessor) {
			resourceManager = accessor.getActiveManager();
		}
		if (resourceManager instanceof LifecycledResourceManagerImplExtension extension) {
			return extension.continuity$getRedirectHandler();
		}
		return null;
	}

	@Nullable
	public static ResourceRedirectHandler get() {
		return get(MinecraftClient.getInstance().getResourceManager());
	}

	public String getSourceSpritePath(String absolutePath) {
		int index = indexMap.getInt(absolutePath);
		if (index == -1) {
			RedirectInfo info = RedirectInfo.of(absolutePath);
			index = nextIndex++;
			redirects.add(info);
			indexMap.put(absolutePath, index);
		}
		return SPRITE_PATH_START + toHex(index);
	}

	public Identifier redirect(Identifier id) {
		String path = id.getPath();
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

		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		Identifier newId = new Identifier(id.getNamespace(), newPath);
		invalidIdentifierState.disable();

		return newId;
	}

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
