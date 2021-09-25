package me.pepperbell.continuity.client.util;

public final class InvalidIdentifierHandler {
	private static final ThreadLocal<State> STATES = ThreadLocal.withInitial(State::new);

	public static boolean areInvalidPathsEnabled() {
		return STATES.get().isEnabled();
	}

	public static void enableInvalidPaths() {
		STATES.get().enable();
	}

	public static void disableInvalidPaths() {
		STATES.get().disable();
	}

	private static class State {
		private int timesEnabled = 0;

		public boolean isEnabled() {
			return timesEnabled > 0;
		}

		public void enable() {
			timesEnabled++;
		}

		public void disable() {
			timesEnabled--;
		}
	}
}
