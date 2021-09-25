package me.pepperbell.continuity.api.client;

import me.pepperbell.continuity.impl.client.CTMStateManagerImpl;

public interface CTMStateManager {
	CTMStateManager INSTANCE = CTMStateManagerImpl.INSTANCE;

	boolean isCTMDisabled();

	void disableCTM();

	void enableCTM();
}
