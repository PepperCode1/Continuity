package me.pepperbell.continuity.client.resource;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public final class EmissiveIdProvider {
	private static final Provider PROVIDER = createProvider();

	private static Provider createProvider() {
		if (FabricLoader.getInstance().isModLoaded("citresewn-defaults")) {
			return EmissiveIdProvider::toEmissiveIdCITR;
		}

		return EmissiveIdProvider::toEmissiveIdStandard;
	}

	@Nullable
	public static Identifier toEmissiveId(Identifier spriteId, String emissiveSuffix) {
		return PROVIDER.toEmissiveId(spriteId, emissiveSuffix);
	}

	@ApiStatus.Internal
	public static void init() {
	}

	@Nullable
	private static Identifier toEmissiveIdStandard(Identifier spriteId, String emissiveSuffix) {
		String path = spriteId.getPath();
		if (!path.endsWith(emissiveSuffix)) {
			return new Identifier(spriteId.getNamespace(), path + emissiveSuffix);
		}
		return null;
	}

	/**
	 * Sprite identifiers never have an extension in vanilla. CIT Resewn adds a png extension to some identifiers and
	 * changes SpriteAtlasTexture#getTexturePath to interpret those identifiers as absolute texture locations. This code
	 * accounts for the possibility of the identifier having a png extension and appends the emissive suffix before it.
	 */
	@Nullable
	private static Identifier toEmissiveIdCITR(Identifier spriteId, String emissiveSuffix) {
		String path = spriteId.getPath();
		boolean hasExtension = path.endsWith(".png");
		if (hasExtension) {
			path = path.substring(0, path.length() - 4);
		}
		if (!path.endsWith(emissiveSuffix)) {
			String emissivePath = path + emissiveSuffix;
			if (hasExtension) {
				emissivePath += ".png";
			}
			return new Identifier(spriteId.getNamespace(), emissivePath);
		}
		return null;
	}

	private interface Provider {
		@Nullable
		Identifier toEmissiveId(Identifier spriteId, String emissiveSuffix);
	}
}
