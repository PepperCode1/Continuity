package me.pepperbell.continuity.client.model;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public class EmissiveUnbakedModel extends WrappingUnbakedModel {
	public EmissiveUnbakedModel(UnbakedModel wrapped) {
		super(wrapped);
	}

	@Nullable
	@Override
	public BakedModel wrapBaked(@Nullable BakedModel bakedWrapped, ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		if (bakedWrapped == null || bakedWrapped.isBuiltin()) {
			return bakedWrapped;
		}
		return new EmissiveBakedModel(bakedWrapped);
	}
}
