package me.pepperbell.continuity.client.resource;

import me.pepperbell.continuity.client.util.BooleanState;

public final class InvalidIdentifierStateHolder {
	private static final ThreadLocal<BooleanState> STATES = ThreadLocal.withInitial(BooleanState::new);

	public static BooleanState get() {
		return STATES.get();
	}
}
