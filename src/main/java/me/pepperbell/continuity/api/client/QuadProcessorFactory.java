package me.pepperbell.continuity.api.client;

import java.util.function.Function;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

public interface QuadProcessorFactory<T extends CTMProperties> {
	QuadProcessor createProcessor(T properties, Function<SpriteIdentifier, Sprite> textureGetter);
}
