package me.pepperbell.continuity.client.handler;

import java.util.List;
import java.util.Map;

import me.pepperbell.continuity.client.event.AddBlockStateModelCallback;
import me.pepperbell.continuity.client.resource.CTMLoadingContainer;
import me.pepperbell.continuity.client.resource.CTMPropertiesLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;

public class AddBlockStateModelCallbackHandler implements AddBlockStateModelCallback {
	// TODO: remove modelId2StateMap map
	private final Map<ModelIdentifier, BlockState> modelId2StateMap;
	private final Map<ModelIdentifier, List<CTMLoadingContainer<?>>> modelId2ContainersMap;

	public AddBlockStateModelCallbackHandler(Map<ModelIdentifier, BlockState> modelId2StateMap, Map<ModelIdentifier, List<CTMLoadingContainer<?>>> modelId2ContainersMap) {
		this.modelId2StateMap = modelId2StateMap;
		this.modelId2ContainersMap = modelId2ContainersMap;
	}

	@Override
	public void onAddBlockStateModel(ModelIdentifier id, BlockState state, UnbakedModel model, ModelLoader modelLoader) {
		modelId2StateMap.put(id, state);
		List<CTMLoadingContainer<?>> containerList = CTMPropertiesLoader.getAllAffecting(state);
		if (containerList != null) {
			modelId2ContainersMap.put(id, containerList);
		}
	}
}
