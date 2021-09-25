package me.pepperbell.continuity.client.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.resource.CTMLoadingContainer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public class CTMUnbakedModel implements UnbakedModel {
	private final UnbakedModel wrapped;
	private final List<CTMLoadingContainer<?>> containerList;
	@Nullable
	private final List<CTMLoadingContainer<?>> multipassContainerList;

	public CTMUnbakedModel(UnbakedModel wrapped, List<CTMLoadingContainer<?>> containerList, @Nullable List<CTMLoadingContainer<?>> multipassContainerList) {
		this.wrapped = wrapped;
		this.containerList = containerList;
		this.multipassContainerList = multipassContainerList;
	}

	@Override
	public Collection<Identifier> getModelDependencies() {
		return wrapped.getModelDependencies();
	}

	@Override
	public Set<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		Set<SpriteIdentifier> dependencies = new ObjectOpenHashSet<>(wrapped.getTextureDependencies(unbakedModelGetter, unresolvedTextureReferences));
		for (CTMLoadingContainer<?> container : containerList) {
			dependencies.addAll(container.getProperties().getTextureDependencies());
		}
		if (multipassContainerList != null) {
			for (CTMLoadingContainer<?> container : multipassContainerList) {
				dependencies.addAll(container.getProperties().getTextureDependencies());
			}
		}
		return dependencies;
	}

	@Nullable
	@Override
	public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		BakedModel bakedWrapped = wrapped.bake(loader, textureGetter, rotationContainer, modelId);
		if (bakedWrapped == null) {
			return null;
		}
		return new CTMBakedModel(bakedWrapped, toProcessorList(containerList, textureGetter), multipassContainerList == null ? null : toProcessorList(multipassContainerList, textureGetter));
	}

	protected static ImmutableList<QuadProcessor> toProcessorList(List<CTMLoadingContainer<?>> containerList, Function<SpriteIdentifier, Sprite> textureGetter) {
		ImmutableList.Builder<QuadProcessor> listBuilder = ImmutableList.builder();
		for (CTMLoadingContainer<?> container : containerList) {
			listBuilder.add(container.toProcessor(textureGetter));
		}
		return listBuilder.build();
	}
}
