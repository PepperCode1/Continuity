package me.pepperbell.continuity.impl.client;

import me.pepperbell.continuity.api.client.CTMStateManager;
import me.pepperbell.continuity.client.model.CTMBakedModel;

public class CTMStateManagerImpl implements CTMStateManager {
	public static final CTMStateManagerImpl INSTANCE = new CTMStateManagerImpl();

	@Override
	public boolean isCTMDisabled() {
		return CTMBakedModel.isCTMDisabled();
	}

	@Override
	public void disableCTM() {
		CTMBakedModel.setCTMDisabled(true);
	}

	@Override
	public void enableCTM() {
		CTMBakedModel.setCTMDisabled(false);
	}
}
