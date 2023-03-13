package me.pepperbell.continuity.client.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.client.mixinterface.SpriteAtlasTextureDataExtension;
import me.pepperbell.continuity.client.model.CTMUnbakedModel;
import me.pepperbell.continuity.client.model.EmissiveUnbakedModel;
import me.pepperbell.continuity.client.util.VoidSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public final class ModelWrappingHandler {
	private static final Map<ModelIdentifier, BlockState> MODEL_ID_2_STATE_MAP = new Object2ObjectOpenHashMap<>();
	private static final Map<ModelIdentifier, List<CTMLoadingContainer<?>>> MODEL_ID_2_CONTAINERS_MAP = new Object2ObjectOpenHashMap<>();

	public static void onAddBlockStateModel(ModelIdentifier id, BlockState state) {
		MODEL_ID_2_STATE_MAP.put(id, state);
		List<CTMLoadingContainer<?>> containerList = CTMPropertiesLoader.getAllAffecting(state);
		if (containerList != null) {
			MODEL_ID_2_CONTAINERS_MAP.put(id, containerList);
		}
	}

	public static void wrapCTMModels(Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake) {
		if (CTMPropertiesLoader.isEmpty()) {
			clearMaps();
			return;
		}

		Map<Identifier, UnbakedModel> wrappedModels = new Object2ObjectOpenHashMap<>();
		Function<Identifier, UnbakedModel> unbakedModelGetter = createUnbakedModelGetter(unbakedModels);
		VoidSet<Pair<String, String>> voidSet = VoidSet.get();
		CollectionBasedConsumer<CTMLoadingContainer<?>> reusableConsumer = new CollectionBasedConsumer<>();

		modelsToBake.forEach((id, model) -> {
			// Only wrap final block state models
			if (id instanceof ModelIdentifier modelId && isBlockStateModelId(modelId)) {
				Collection<SpriteIdentifier> dependencies;
				try {
					dependencies = model.getTextureDependencies(unbakedModelGetter, voidSet);
				} catch (ModelNotLoadedException e) {
					return;
				}

				List<CTMLoadingContainer<?>> containerList = MODEL_ID_2_CONTAINERS_MAP.get(modelId);
				if (containerList == null) {
					containerList = CTMPropertiesLoader.getAllAffecting(dependencies);
					if (containerList == null) {
						return;
					}
				} else {
					reusableConsumer.setCollection(containerList);
					CTMPropertiesLoader.consumeAllAffecting(dependencies, reusableConsumer);
				}
				containerList.sort(Collections.reverseOrder());

				Set<CTMLoadingContainer<?>> multipassContainerSet = null;
				int amount = containerList.size();
				for (int i = 0; i < amount; i++) {
					CTMLoadingContainer<?> container = containerList.get(i);
					Set<CTMLoadingContainer<?>> dependents = container.getRecursiveMultipassDependents();
					if (dependents != null) {
						if (multipassContainerSet == null) {
							multipassContainerSet = new ObjectOpenHashSet<>();
						}
						multipassContainerSet.addAll(dependents);
					}
				}
				List<CTMLoadingContainer<?>> multipassContainerList = null;
				if (multipassContainerSet != null) {
					BlockState state = MODEL_ID_2_STATE_MAP.get(modelId);
					for (CTMLoadingContainer<?> container : multipassContainerSet) {
						if (!container.getProperties().affectsBlockStates() || container.getProperties().affectsBlockState(state)) {
							if (multipassContainerList == null) {
								multipassContainerList = new ObjectArrayList<>();
							}
							multipassContainerList.add(container);
						}
					}
					if (multipassContainerList != null) {
						multipassContainerList.sort(Collections.reverseOrder());
					}
				}

				wrappedModels.put(modelId, new CTMUnbakedModel(model, containerList, multipassContainerList));
			}
		});

		clearMaps();
		injectWrappedModels(wrappedModels, unbakedModels, modelsToBake);
	}

	public static void wrapEmissiveModels(Map<Identifier, Pair<SpriteAtlasTexture, SpriteAtlasTexture.Data>> spriteAtlasData, Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake) {
		Set<SpriteIdentifier> spriteIdsToWrap = new ObjectOpenHashSet<>();

		spriteAtlasData.forEach((atlasId, pair) -> {
			SpriteAtlasTexture.Data data = pair.getSecond();
			Map<Identifier, Identifier> emissiveIdMap = ((SpriteAtlasTextureDataExtension) data).continuity$getEmissiveIdMap();
			if (emissiveIdMap != null) {
				for (Identifier id : emissiveIdMap.keySet()) {
					spriteIdsToWrap.add(new SpriteIdentifier(atlasId, id));
				}
			}
		});

		if (spriteIdsToWrap.isEmpty()) {
			return;
		}

		Map<Identifier, UnbakedModel> wrappedModels = new Object2ObjectOpenHashMap<>();
		Function<Identifier, UnbakedModel> unbakedModelGetter = createUnbakedModelGetter(unbakedModels);
		VoidSet<Pair<String, String>> voidSet = VoidSet.get();

		unbakedModels.forEach((id, model) -> {
			Collection<SpriteIdentifier> dependencies;
			try {
				dependencies = model.getTextureDependencies(unbakedModelGetter, voidSet);
			} catch (ModelNotLoadedException e) {
				return;
			}

			for (SpriteIdentifier spriteId : dependencies) {
				if (spriteIdsToWrap.contains(spriteId)) {
					wrappedModels.put(id, new EmissiveUnbakedModel(model));
					return;
				}
			}
		});

		injectWrappedModels(wrappedModels, unbakedModels, modelsToBake);
	}

	private static Function<Identifier, UnbakedModel> createUnbakedModelGetter(Map<Identifier, UnbakedModel> unbakedModels) {
		return id -> {
			UnbakedModel model = unbakedModels.get(id);
			if (model == null) {
				throw new ModelNotLoadedException();
			}
			return model;
		};
	}

	private static void injectWrappedModels(Map<Identifier, UnbakedModel> wrappedModels, Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake) {
		wrappedModels.forEach((id, wrapped) -> {
			unbakedModels.replace(id, wrapped);
			modelsToBake.replace(id, wrapped);
		});
	}

	private static boolean isBlockStateModelId(ModelIdentifier id) {
		return !id.getVariant().equals("inventory");
	}

	private static void clearMaps() {
		MODEL_ID_2_STATE_MAP.clear();
		MODEL_ID_2_CONTAINERS_MAP.clear();
	}

	private static class ModelNotLoadedException extends RuntimeException {
	}

	private static class CollectionBasedConsumer<T> implements Consumer<T> {
		private Collection<T> collection;

		@Override
		public void accept(T t) {
			collection.add(t);
		}

		public void setCollection(Collection<T> collection) {
			this.collection = collection;
		}
	}
}
