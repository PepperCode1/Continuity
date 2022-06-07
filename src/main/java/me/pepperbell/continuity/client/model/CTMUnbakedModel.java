package me.pepperbell.continuity.client.model;

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

public class CTMUnbakedModel extends WrappingUnbakedModel {
	private final List<CTMLoadingContainer<?>> containerList;
	@Nullable
	private final List<CTMLoadingContainer<?>> multipassContainerList;

	public CTMUnbakedModel(UnbakedModel wrapped, List<CTMLoadingContainer<?>> containerList, @Nullable List<CTMLoadingContainer<?>> multipassContainerList) {
		super(wrapped);
		this.containerList = containerList;
		this.multipassContainerList = multipassContainerList;
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
	public BakedModel wrapBaked(@Nullable BakedModel bakedWrapped, ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		if (bakedWrapped == null || bakedWrapped.isBuiltin()) {
			return bakedWrapped;
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
