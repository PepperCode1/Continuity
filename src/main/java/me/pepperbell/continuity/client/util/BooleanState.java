package me.pepperbell.continuity.client.util;

public class BooleanState {
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
