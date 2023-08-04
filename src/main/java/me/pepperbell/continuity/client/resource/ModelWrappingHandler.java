package me.pepperbell.continuity.client.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

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

public class ModelWrappingHandler {
	private final CTMPropertiesLoader.LoadingResult ctmLoadingResult;

	private final Map<ModelIdentifier, BlockState> modelId2StateMap = new Object2ObjectOpenHashMap<>();

	public ModelWrappingHandler(CTMPropertiesLoader.LoadingResult ctmLoadingResult) {
		this.ctmLoadingResult = ctmLoadingResult;
	}

	public void onAddBlockStateModel(ModelIdentifier id, BlockState state) {
		if (!ctmLoadingResult.isEmpty()) {
			modelId2StateMap.put(id, state);
		}
	}

	public void wrapCTMModels(Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake) {
		if (ctmLoadingResult.isEmpty()) {
			return;
		}

		Map<Identifier, UnbakedModel> wrappedModels = new Object2ObjectOpenHashMap<>();
		Function<Identifier, UnbakedModel> unbakedModelGetter = createUnbakedModelGetter(unbakedModels);
		VoidSet<Pair<String, String>> voidSet = VoidSet.get();
		OptionalListCreator<CTMLoadingContainer<?>> listCreator = new OptionalListCreator<>();

		modelsToBake.forEach((id, model) -> {
			// Only wrap top-level block state models
			if (!(id instanceof ModelIdentifier modelId) || !isBlockStateModelId(modelId)) {
				return;
			}

			BlockState state = modelId2StateMap.get(modelId);
			if (state == null) {
				return;
			}

			Collection<SpriteIdentifier> spriteIds;
			try {
				spriteIds = model.getTextureDependencies(unbakedModelGetter, voidSet);
			} catch (ModelNotLoadedException e) {
				return;
			}

			ctmLoadingResult.consumeAllAffecting(state, listCreator);
			ctmLoadingResult.consumeAllAffecting(spriteIds, listCreator);
			List<CTMLoadingContainer<?>> containerList = listCreator.get();
			if (containerList == null) {
				return;
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
		});

		injectWrappedModels(wrappedModels, unbakedModels, modelsToBake);
	}

	public void wrapEmissiveModels(Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake, Map<Identifier, Pair<SpriteAtlasTexture, SpriteAtlasTexture.Data>> spriteAtlasData) {
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
			Collection<SpriteIdentifier> spriteIds;
			try {
				spriteIds = model.getTextureDependencies(unbakedModelGetter, voidSet);
			} catch (ModelNotLoadedException e) {
				return;
			}

			for (SpriteIdentifier spriteId : spriteIds) {
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

	private static class ModelNotLoadedException extends RuntimeException {
	}

	private static class OptionalListCreator<T> implements Consumer<T> {
		private ObjectArrayList<T> list = null;

		@Override
		public void accept(T t) {
			if (list == null) {
				list = new ObjectArrayList<>();
			}
			list.add(t);
		}

		@Nullable
		public ObjectArrayList<T> get() {
			ObjectArrayList<T> list = this.list;
			this.list = null;
			return list;
		}
	}
}
