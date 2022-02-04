package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.function.Function;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.api.client.QuadProcessorFactory;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

public abstract class AbstractQuadProcessorFactory<T extends BaseCTMProperties> implements QuadProcessorFactory<T> {
	@Override
	public QuadProcessor createProcessor(T properties, Function<SpriteIdentifier, Sprite> textureGetter) {
		int textureAmount = getTextureAmount(properties);
		List<SpriteIdentifier> spriteIds = properties.getSpriteIds();
		int provided = spriteIds.size();
		int max = provided;

		if (provided > textureAmount) {
			ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but " + provided + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
			max = textureAmount;
		}

		Sprite[] sprites = new Sprite[textureAmount];
		Sprite missingSprite = textureGetter.apply(TextureUtil.MISSING_SPRITE_ID);
		boolean supportsNullSprites = supportsNullSprites(properties);
		for (int i = 0; i < max; i++) {
			Sprite sprite;
			SpriteIdentifier spriteId = spriteIds.get(i);
			if (spriteId.equals(BaseCTMProperties.SPECIAL_SKIP_SPRITE_ID)) {
				sprite = missingSprite;
			} else if (spriteId.equals(BaseCTMProperties.SPECIAL_DEFAULT_SPRITE_ID)) {
				sprite = supportsNullSprites ? null : missingSprite;
			} else {
				sprite = textureGetter.apply(spriteId);
			}
			sprites[i] = sprite;
		}

		if (provided < textureAmount) {
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but only " + provided + " were provided in file '" + properties.getId() + "' in pack '" + properties.getPackName() + "'");
			for (int i = provided; i < textureAmount; i++) {
				sprites[i] = missingSprite;
			}
		}

		return createProcessor(properties, sprites);
	}

	public abstract QuadProcessor createProcessor(T properties, Sprite[] sprites);

	public abstract int getTextureAmount(T properties);

	public boolean supportsNullSprites(T properties) {
		return true;
	}
}
