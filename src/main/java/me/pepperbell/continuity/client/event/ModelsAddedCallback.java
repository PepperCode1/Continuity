package me.pepperbell.continuity.client.event;

import java.util.Map;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public interface ModelsAddedCallback {
	Event<ModelsAddedCallback> EVENT = EventFactory.createArrayBacked(ModelsAddedCallback.class,
			listeners -> (modelLoader, resourceManager, profiler, unbakedModels, modelsToBake) -> {
				for (ModelsAddedCallback callback : listeners) {
					callback.onModelsAdded(modelLoader, resourceManager, profiler, unbakedModels, modelsToBake);
				}
			}
	);

	void onModelsAdded(ModelLoader modelLoader, ResourceManager resourceManager, Profiler profiler, Map<Identifier, UnbakedModel> unbakedModels, Map<Identifier, UnbakedModel> modelsToBake);
}
