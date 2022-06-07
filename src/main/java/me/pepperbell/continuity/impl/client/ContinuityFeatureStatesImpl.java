package me.pepperbell.continuity.impl.client;

import me.pepperbell.continuity.api.client.ContinuityFeatureStates;
import me.pepperbell.continuity.client.model.ModelObjectsContainer;
import me.pepperbell.continuity.client.util.BooleanState;

public class ContinuityFeatureStatesImpl implements ContinuityFeatureStates {
	private final FeatureStateImpl connectedTexturesState = new FeatureStateImpl();
	private final FeatureStateImpl emissiveTexturesState = new FeatureStateImpl();
	{
		connectedTexturesState.enable();
		emissiveTexturesState.enable();
	}

	public static ContinuityFeatureStatesImpl get() {
		return ModelObjectsContainer.get().featureStates;
	}

	@Override
	public FeatureState getConnectedTexturesState() {
		return connectedTexturesState;
	}

	@Override
	public FeatureState getEmissiveTexturesState() {
		return emissiveTexturesState;
	}

	public static class FeatureStateImpl extends BooleanState implements FeatureState {
	}
}
