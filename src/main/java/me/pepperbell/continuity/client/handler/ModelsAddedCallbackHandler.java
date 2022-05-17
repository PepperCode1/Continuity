package me.pepperbell.continuity.client.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.pepperbell.continuity.client.event.ModelsAddedCallback;
import me.pepperbell.continuity.client.model.CTMUnbakedModel;
import me.pepperbell.continuity.client.resource.CTMLoadingContainer;
import me.pepperbell.continuity.client.resource.CTMPropertiesLoader;
import me.pepperbell.continuity.client.util.VoidSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public class ModelsAddedCallbackHandler implements ModelsAddedCallback {
	private final Map<ModelIdentifier, BlockState> modelId2StateMap;
	private final Map<ModelIdentifier, List<CTMLoadingContainer<?>>> modelId2ContainersMap;

	public ModelsAddedCallbackHandler(Map<ModelIdentifier, BlockState> modelId2StateMap, Map<ModelIdentifier, List<CTMLoadingContainer<?>>> modelId2ContainersMap) {
		this.modelId2StateMap = modelId2StateMap;
		this.modelId2ContainersMap = modelId2ContainersMap;
	}

	private static class NotLoadedModelException extends RuntimeException {}

	@Override
	public void onModelsAdded(ModelLoader modelLoader, ResourceManager resourceManager, Profiler profiler, Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake) {
		Object2ObjectOpenHashMap<Identifier, UnbakedModel> wrappedModels = new Object2ObjectOpenHashMap<>();

		Function<Identifier, UnbakedModel> unbakedModelGetter = id -> {
			UnbakedModel model = unbakedModels.get(id);
			if (model == null) {
				throw new NotLoadedModelException();
			}
			return model;
		};
		VoidSet<Pair<String, String>> voidSet = VoidSet.get();
		CollectionBasedConsumer<CTMLoadingContainer<?>> reusableConsumer = new CollectionBasedConsumer<>();

		// Check which models should be wrapped
		for (Map.Entry<Identifier, UnbakedModel> entry : unbakedModels.entrySet()) {
			if (entry.getKey() instanceof ModelIdentifier id) {
				// Only wrap final block state models
				if (isBlockStateModelId(id)) {
					UnbakedModel model = entry.getValue();
					Collection<SpriteIdentifier> dependencies;

					try{
						dependencies = model.getTextureDependencies(unbakedModelGetter, voidSet);
					} catch(NotLoadedModelException exception){
						continue;
					}

					List<CTMLoadingContainer<?>> containerList = modelId2ContainersMap.get(id);
					if (containerList == null) {
						containerList = CTMPropertiesLoader.getAllAffecting(dependencies);
						if (containerList == null) {
							continue;
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
								multipassContainerSet = new ObjectArraySet<>();
							}
							multipassContainerSet.addAll(dependents);
						}
					}
					List<CTMLoadingContainer<?>> multipassContainerList = null;
					if (multipassContainerSet != null) {
						BlockState state = modelId2StateMap.get(id);
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

					wrappedModels.put(id, new CTMUnbakedModel(model, containerList, multipassContainerList));
				}
			}
		}

		modelId2StateMap.clear();
		modelId2ContainersMap.clear();

		// Inject wrapped models
		ObjectIterator<Object2ObjectMap.Entry<Identifier, UnbakedModel>> iterator = wrappedModels.object2ObjectEntrySet().fastIterator();
		while (iterator.hasNext()) {
			Object2ObjectMap.Entry<Identifier, UnbakedModel> entry = iterator.next();
			Identifier id = entry.getKey();
			UnbakedModel wrapped = entry.getValue();

			unbakedModels.put(id, wrapped);
			if (modelsToBake.containsKey(id)) {
				modelsToBake.put(id, wrapped);
			}
		}
	}

	private boolean isBlockStateModelId(ModelIdentifier id) {
		return !id.getVariant().equals("inventory");
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
