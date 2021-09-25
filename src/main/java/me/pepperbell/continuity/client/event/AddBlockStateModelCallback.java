package me.pepperbell.continuity.client.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;

public interface AddBlockStateModelCallback {
	Event<AddBlockStateModelCallback> EVENT = EventFactory.createArrayBacked(AddBlockStateModelCallback.class,
			listeners -> (id, state, model, modelLoader) -> {
				for (AddBlockStateModelCallback callback : listeners) {
					callback.onAddBlockStateModel(id, state, model, modelLoader);
				}
			}
	);

	void onAddBlockStateModel(ModelIdentifier id, BlockState state, UnbakedModel model, ModelLoader modelLoader);
}
