package me.pepperbell.continuity.client.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import me.pepperbell.continuity.client.mixin.ModelLoaderAccessor;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public abstract class WrappingUnbakedModel implements UnbakedModel {
	protected final UnbakedModel wrapped;
	protected boolean isBaking;

	public WrappingUnbakedModel(UnbakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public Collection<Identifier> getModelDependencies() {
		return wrapped.getModelDependencies();
	}

	@Override
	public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		return wrapped.getTextureDependencies(unbakedModelGetter, unresolvedTextureReferences);
	}

	@Override
	@Nullable
	public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		if (isBaking) {
			return null;
		}
		isBaking = true;

		Map<Identifier, UnbakedModel> unbakedModels = ((ModelLoaderAccessor) loader).getUnbakedModels();
		UnbakedModel previous = unbakedModels.replace(modelId, wrapped);
		BakedModel bakedWrapped = loader.bake(modelId, rotationContainer);
		unbakedModels.replace(modelId, previous);

		BakedModel baked = wrapBaked(bakedWrapped, loader, textureGetter, rotationContainer, modelId);
		isBaking = false;
		return baked;
	}

	@Nullable
	public abstract BakedModel wrapBaked(@Nullable BakedModel bakedWrapped, ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId);
}
